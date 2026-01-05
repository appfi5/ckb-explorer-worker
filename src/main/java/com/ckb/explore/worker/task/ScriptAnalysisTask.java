package com.ckb.explore.worker.task;

import com.ckb.explore.worker.config.ScriptConfig;
import com.ckb.explore.worker.entity.*;
import com.ckb.explore.worker.enums.CellType;
import com.ckb.explore.worker.enums.HashType;
import com.ckb.explore.worker.enums.LockType;
import com.ckb.explore.worker.enums.NftType;
import com.ckb.explore.worker.mapper.*;
import com.ckb.explore.worker.utils.ByteUtils;
import com.ckb.explore.worker.utils.CkbUtil;
import com.ckb.explore.worker.utils.TypeConversionUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.nervos.ckb.utils.Numeric;
import org.redisson.api.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ScriptAnalysisTask {

    @Resource
    RedissonClient redissonClient;

    @Resource
    ScriptMapper scriptMapper;

    @Resource
    OutputMapper outputMapper;

    @Resource
    DobExtendMapper dobExtendMapper;

    @Resource
    DobCodeMapper dobCodeMapper;

    @Resource
    LockScriptExtendMapper lockScriptExtendMapper;

    @Resource
    TypeScriptExtendMapper typeScriptExtendMapper;

    @Resource
    OutputDataMapper outputDataMapper;

    @Resource OmigaInscriptionInfoMapper omigaInscriptionInfoMapper;

    private static final int BATCH_SIZE = 1000;

    private static final int SPORE_BATCH_SIZE = 100;



    private static final String SPORE_TASK_SCRIPT_ID = "SPORE_TASK_SCRIPT_ID";

    private static final String ERROR_SPORE_TASK_SCRIPT_ID = "ERROR_SPORE_TASK_SCRIPT_ID";

    private static final String SCRIPT_TASK_SCRIPT_ID = "SCRIPT_TASK_SCRIPT_ID";

    private static final Pattern CHINESE_ONLY_PATTERN = Pattern.compile("^[\\u4E00-\\u9FFF]+$");
    private static final Pattern VISIBLE_CHAR_PATTERN = Pattern.compile("^[\\x21-\\x7E\\u4E00-\\u9FFF]+(?:\\s[\\x21-\\x7E\\u4E00-\\u9FFF]+)*$");

    private static final String M_NFT_CLASS_TASK_SCRIPT_ID = "M_NFT_CLASS_TASK_SCRIPT_ID";

    private static final String M_NFT_TOKEN_TASK_SCRIPT_ID = "M_NFT_TOKEN_TASK_SCRIPT_ID";


    private static final String OMIGA_TASK_SCRIPT_ID = "OMIGA_TASK_SCRIPT_ID";



    @Resource
    ScriptConfig scriptConfig;

    @Scheduled(cron = "${cron.sporeTask:0 */1 * * * ?}")
    public void sporeTask() {
        RLock lock = redissonClient.getLock("sporeTask");
        Boolean isLock = false;
        try {
            isLock = lock.tryLock(5, 30, TimeUnit.SECONDS);
            log.info("sporeTask start isLock is {}", isLock);
            if (!isLock) {
                return;
            }
            RAtomicLong sporeTaskId = redissonClient.getAtomicLong(SPORE_TASK_SCRIPT_ID);
            long startId = sporeTaskId.get();

            if (startId == 0) {
                Long maxDobExtendId = dobExtendMapper.getMaxId();
                Long maxDobCodeScriptId = dobCodeMapper.getMaxScriptId();
                if (maxDobExtendId != null) {
                    startId = maxDobExtendId;
                }
                if (maxDobCodeScriptId != null) {
                    startId = Math.max(startId, maxDobCodeScriptId);
                }
            }

            List<byte[]> codeHashes = new ArrayList<>();
            scriptConfig.getTypeScripts().forEach(typeScript -> {
                if(Objects.equals(typeScript.getCellType(),CellType.SPORE_CLUSTER.getValue())
                        ||Objects.equals(typeScript.getCellType(),CellType.SPORE_CELL.getValue())
                        ||Objects.equals(typeScript.getCellType(),CellType.DID_CELL.getValue())){
                    codeHashes.add(Numeric.hexStringToByteArray(typeScript.getCodeHash()));
                }
            });

            List<Script> scripts = scriptMapper.getBatchCodeHashesAndId(startId,codeHashes, SPORE_BATCH_SIZE);
            if (scripts.isEmpty()) {
                return;
            }

            Long maxScriptId = scripts.stream().mapToLong(Script::getId).max().orElse(startId);
            for (Script script : scripts) {

                dealNft(script);
            }
            sporeTaskId.set(maxScriptId);
        } catch (Exception e) {
            log.error("Error in sporeTask", e);
        } finally {
          if (isLock && lock.isHeldByCurrentThread()) {
            lock.unlock();
          }
        }


    }

    @Scheduled(cron = "${cron.sporeErrorTask:0 */3 * * * ?}")
    public void sporeErrorTask() {
        RLock lock = redissonClient.getLock("sporeErrorTask");
        Boolean isLock = false;
        try {
            isLock = lock.tryLock(5, 30, TimeUnit.SECONDS);
            log.info("sporeErrorTask start isLock is {}", isLock);
            if (!isLock) {
                return;
            }
            RSet<Long> errorTaskScriptIds = redissonClient.getSet(ERROR_SPORE_TASK_SCRIPT_ID);
            if (errorTaskScriptIds.isEmpty()) {
                return;
            }
            errorTaskScriptIds.forEach(errorTaskScriptId -> {
                Script script = scriptMapper.selectById(errorTaskScriptId);
                errorTaskScriptIds.remove(errorTaskScriptId);
                dealNft(script);
            });

        } catch (Exception e) {
            log.error("Error in sporeErrorTask", e);
        } finally {
          if (isLock && lock.isHeldByCurrentThread()) {
            lock.unlock();
          }
        }
    }


    @Scheduled(cron = "${cron.scriptTypeTask:0/10 * * * * ?}")
    public void scriptTypeTask() {
        RLock lock = redissonClient.getLock("scriptTypeTask");
        Boolean isLock = false;
        try {
            isLock = lock.tryLock(5, 30, TimeUnit.SECONDS);
            log.info("scriptTypeTask start isLock is {}", isLock);
            if (!isLock) {
                return;
            }
            RAtomicLong scriptTaskId = redissonClient.getAtomicLong(SCRIPT_TASK_SCRIPT_ID);
            long startId = scriptTaskId.get();
            List<Script> scripts = scriptMapper.getBatchById(startId, BATCH_SIZE);
            if (scripts.isEmpty()) {
                return;
            }

            Long maxScriptId = scripts.stream().mapToLong(Script::getId).max().orElse(startId);
            for (Script script : scripts) {
                dealScriptType(script);
            }
            scriptTaskId.set(maxScriptId);
        } catch (Exception e) {
            log.error("Error in scriptTypeTask ", e);
        } finally {
          if (isLock && lock.isHeldByCurrentThread()) {
            lock.unlock();
          }
        }


    }


    private void dealNft(Script script) {
        RSet<Long> errorTaskScriptId = redissonClient.getSet(ERROR_SPORE_TASK_SCRIPT_ID);
        try {
            //非typescript 不处理
            if(script.getIsTypescript()==0){
                return;
            }
            String codeHash = Numeric.toHexString(script.getCodeHash());
            Integer hashType = script.getHashType().intValue();
            ScriptConfig.TypeScript  typeScript = scriptConfig.getTypeScriptByCodeHash(codeHash,null);
            if(typeScript==null){
                return;
            }
            boolean isSporeCluster = Objects.equals(typeScript.getCellType(),CellType.SPORE_CLUSTER.getValue()) &&  HashType.DATA1.getCode() == hashType;
            boolean isMnftClass = Objects.equals(typeScript.getCellType(),CellType.M_NFT_CLASS.getValue()) &&  HashType.TYPE.getCode() == hashType;
            boolean isSpore = Objects.equals(typeScript.getCellType(),CellType.SPORE_CELL.getValue()) && HashType.DATA1.getCode() == hashType;
            boolean isDid = Objects.equals(typeScript.getCellType(),CellType.DID_CELL.getValue()) && HashType.TYPE.getCode() == hashType;
            boolean isMnftToken = Objects.equals(typeScript.getCellType(),CellType.M_NFT_TOKEN.getValue()) && HashType.TYPE.getCode() == hashType;
            if ( isSporeCluster || isMnftClass) {
                //处理dobCode信息
                Output output;
                if(isMnftClass){
                    output = outputMapper.selectLiveCellByTypeScriptId(script.getId());
                }else {
                    //dob会因为此处cell变更需要重新跑么?
                    output   = outputMapper.selectOneByTypeScriptId(script.getId());
                }
                //mnftClass可能存在不活跃数据
                if(output==null){
                    if(!isMnftClass){
                        errorTaskScriptId.add(script.getId());
                    }
                    return;
                }

                DobExtend dobExtend = new DobExtend();
                if(isSporeCluster) {
                    //sporeCluster 中args为clusterId,非集群模式的统一在初始化sql里处理
                    if (script.getArgs() == null || script.getArgs().length == 0) {
                        return;
                    }
                    CkbUtil.SporeClusterData sporeClusterData = CkbUtil.parseSporeClusterData(Numeric.toHexString(getOutputData(output)));
                    dobExtend.setName(sporeClusterData.getName());
                    dobExtend.setDescription(sporeClusterData.getDescription());
                    dobExtend.setStandard(NftType.DOB.getCode());
                }else if(isMnftClass){
                    CkbUtil.TokenClassData tokenClassData = CkbUtil.parseTokenClassData(Numeric.toHexString(getOutputData(output)));
                    dobExtend.setName(tokenClassData.getName());
                    dobExtend.setDescription(tokenClassData.getDescription());
                    dobExtend.setIconUrl(tokenClassData.getRenderer());
                    dobExtend.setStandard(NftType.M_NFT.getCode());
                }
                dobExtend.setId(script.getId());
                dobExtend.setDobScriptId(script.getId());
                dobExtend.setArgs(script.getArgs());
                dobExtend.setDobScriptHash(script.getScriptHash());

                dobExtend.setLockScriptId(output.getLockScriptId());
                Script lockScript = scriptMapper.selectById(output.getLockScriptId());
                dobExtend.setBlockTimestamp(script.getTimestamp());
                dobExtend.setDobCodeHash(script.getCodeHash());
                if (lockScript != null) {
                    dobExtend.setCreator(TypeConversionUtil.scriptToAddress(lockScript.getCodeHash(), lockScript.getArgs(), lockScript.getHashType()));
                }
                dobExtendMapper.insert(dobExtend);
                dobExtendMapper.flush();
                String tags = getTags(dobExtend.getName(),lockScript);
                dobExtendMapper.updateTagsById(dobExtend.getId(),tags);
            } else if (isSpore || isDid || isMnftToken) {
                     // 处理sporeNft
                    DobCode dobCode = new DobCode();
                    dobCode.setDobCodeScriptId(script.getId());
                    dobCode.setDobCodeScriptArgs(script.getArgs());
                    Output output = outputMapper.selectOneByTypeScriptId(script.getId());
                    if(output==null){
                        errorTaskScriptId.add(script.getId());
                        return;
                    }
                    if(isDid||isSpore){
                        CkbUtil.SporeCellData sporeCellData = CkbUtil.parseSporeCellData(Numeric.toHexString(getOutputData(output)));
                        if (StringUtils.hasLength(sporeCellData.getClusterId())) {
                            DobExtend dobExtend = dobExtendMapper.selectByArgs( Numeric.hexStringToByteArray(sporeCellData.getClusterId()));
                            if (dobExtend == null) {
                                //dobExtend未同步到 则计入错误数据 之后重试
                                errorTaskScriptId.add(script.getId());
                                return;
                            }
                            dobCode.setDobExtendId(dobExtend.getId());

                        } else {
                            //默认初始化的非集群的DobExtend
                            dobCode.setDobExtendId(0L);
                        }
                    }else if (isMnftToken){
                        if(script.getArgs().length<24){
                            return;
                        }
                        DobExtend dobExtend = dobExtendMapper.selectByArgs(Arrays.copyOf(script.getArgs(),24) );
                        if (dobExtend == null) {
                            //dobExtend未同步到 则计入错误数据 之后重试
                            errorTaskScriptId.add(script.getId());
                            return;
                        }
                        dobCode.setDobExtendId(dobExtend.getId());
                    }

                    dobCodeMapper.insert(dobCode);
                }
        } catch (Exception e) {
            log.error("deal script error script {}", script.getId(), e);
            errorTaskScriptId.add(script.getId());
        }
    }

    private String getTags(String name, Script lockScript) {

        if (isInvalidChar(name)) {
            return "invalid";
        } else if (isInvisibleChar(name)) {
            return "suspicious";
        } else if (CkbUtil.outOfLength(name)) {
            return "out-of-length-range";
        } else if (rgppLock(lockScript)) {
            return "supply-limited";
        } else if (singleUseLock(lockScript)) {
            return "rgb++,layer-1-asset";
        } else {
            return "rgb++,layer-2-asset";
        }

    }

    private boolean rgppLock(Script lockScript) {
        ScriptConfig.LockScript config = scriptConfig.getLockScriptByCodeHash(Numeric.toHexString(lockScript.getCodeHash()));
        if (config != null && Objects.equals(config.getName(), LockType.RgbppLock.getValue())) {
            return true;
        }
        return false;
    }

    private boolean singleUseLock(Script lockScript) {
        ScriptConfig.LockScript config = scriptConfig.getLockScriptByCodeHash(Numeric.toHexString(lockScript.getCodeHash()));
        if (config != null && Objects.equals(config.getName(), LockType.SingleUseLock.getValue())) {
            return true;
        }
        return false;
    }


    private void dealScriptType(Script script) {
        if(script.getIsTypescript()==0){
            ScriptConfig.LockScript lockScript = scriptConfig.getLockScriptByCodeHash(Numeric.toHexString(script.getCodeHash()));
            if (lockScript != null && !Objects.equals(lockScript.getName(), LockType.SECP256K1Blake160.getValue())) {
                LockScriptExtend lockScriptExtend = new LockScriptExtend();
                lockScriptExtend.setScriptId(script.getId());
                lockScriptExtend.setLockType(LockType.getCodeByValue(lockScript.getName()));
                lockScriptExtendMapper.insert(lockScriptExtend);
            }
        }else {
            //匹配codeHash
            ScriptConfig.TypeScript typeScript = scriptConfig.getTypeScriptByCodeHash(Numeric.toHexString(script.getCodeHash()),null);
            if (typeScript != null && typeScript.getCellType() != null) {
                if (CellType.valueOf(typeScript.getCellType()).isUdtType()) {
                    if(isUdt(script, typeScript.getCellType())){
                       buildUdt(script,typeScript.getCellType());
                    }
                }
            }
        }


    }

    private Boolean isUdt(Script script,Integer cellTye){
        if(Objects.equals(cellTye,CellType.XUDT_COMPATIBLE.getValue())&&script.getHashType()==HashType.TYPE.getCode()){
            return  true;
        }else if(Objects.equals(cellTye,CellType.XUDT.getValue())&&(script.getHashType()==HashType.TYPE.getCode()||script.getHashType()==HashType.DATA1.getCode())){
           // 可能是omiga  看是否要排除
            return true;
        }else if(Objects.equals(cellTye,CellType.UDT.getValue())&&(script.getHashType()==HashType.TYPE.getCode()||script.getHashType()==HashType.DATA.getCode())){
            Output output = outputMapper.selectOneByTypeScriptId(script.getId());
            byte[] outputData = getOutputData(output);
            if(outputData!=null&outputData.length>=16){
                return true;
            }
        }
        return false;

    }

    private byte[]  getOutputData(Output output){
        byte[] data = output.getData();
        //当data为空时,需判断
        if (!ByteUtils.hasLength(data)) {
            OutputData outputData = outputDataMapper.selectByOutputId(output.getId());
            if (outputData != null) {
                data = outputData.getData();
            }
        }
        return  data;
    }

    private static boolean isInvalidChar(String name) {
        if(!StringUtils.hasLength(name)){
            return false;
        }
        // 判断是否包含非ASCII字符
        boolean hasNonAscii = !name.matches("^[\\x00-\\x7F]*$");
        // 判断是否不是纯中文字符
        boolean notChineseOnly = !CHINESE_ONLY_PATTERN.matcher(name).matches();
        return hasNonAscii && notChineseOnly;
    }


    private static boolean isInvisibleChar(String name) {
        if(!StringUtils.hasLength(name)){
            return false;
        }
        return !VISIBLE_CHAR_PATTERN.matcher(name).matches();
    }



    @Scheduled(cron = "${cron.mNftTask:0 */1 * * * ?}")
    public void mNftTask() {
        RLock lock = redissonClient.getLock("mNftTask");
        Boolean isLock = false;
        try {
            isLock = lock.tryLock(5, 30, TimeUnit.SECONDS);
            log.info("mNftTask start isLock is {}", isLock);
            if (!isLock) {
                return;
            }
            RAtomicLong mNftTaskId = redissonClient.getAtomicLong(M_NFT_CLASS_TASK_SCRIPT_ID);
            long startId = mNftTaskId.get();

            List<byte[]> codeHashes = new ArrayList<>();
            scriptConfig.getTypeScripts().forEach(typeScript -> {
                if(Objects.equals(typeScript.getCellType(),CellType.M_NFT_CLASS.getValue())) {
                    codeHashes.add(Numeric.hexStringToByteArray(typeScript.getCodeHash()));
                }
            });

            List<Script> scripts = scriptMapper.getBatchCodeHashesAndId(startId,codeHashes, SPORE_BATCH_SIZE);
            if (scripts.isEmpty()) {
                return;
            }

            Long maxScriptId = scripts.stream().mapToLong(Script::getId).max().orElse(startId);
            for (Script script : scripts) {
                dealNft(script);
            }
            mNftTaskId.set(maxScriptId);
        } catch (Exception e) {
            log.error("Error in sporeTask", e);
        } finally {
            if (isLock && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    @Scheduled(cron = "${cron.mNftTokenTask:0 */1 * * * ?}")
    public void mNftTokenTask() {
        RLock lock = redissonClient.getLock("mNftTokenTask");
        Boolean isLock = false;
        try {
            isLock = lock.tryLock(5, 30, TimeUnit.SECONDS);
            log.info("mNftTokenTask start isLock is {}", isLock);
            if (!isLock) {
                return;
            }
            RAtomicLong mNftTokenTaskId = redissonClient.getAtomicLong(M_NFT_TOKEN_TASK_SCRIPT_ID);
            long startId = mNftTokenTaskId.get();

            List<byte[]> codeHashes = new ArrayList<>();
            scriptConfig.getTypeScripts().forEach(typeScript -> {
                if(Objects.equals(typeScript.getCellType(),CellType.M_NFT_TOKEN.getValue())){
                    codeHashes.add(Numeric.hexStringToByteArray(typeScript.getCodeHash()));
                }
            });

            List<Script> scripts = scriptMapper.getBatchCodeHashesAndId(startId,codeHashes, SPORE_BATCH_SIZE);
            if (scripts.isEmpty()) {
                return;
            }

            Long maxScriptId = scripts.stream().mapToLong(Script::getId).max().orElse(startId);
            for (Script script : scripts) {
                dealNft(script);
            }
            mNftTokenTaskId.set(maxScriptId);
        } catch (Exception e) {
            log.error("Error in sporeTask", e);
        } finally {
            if (isLock && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }




    private void buildUdt(Script script,Integer cellTye){
        TypeScriptExtend typeScriptExtend = new TypeScriptExtend();
        typeScriptExtend.setScriptId(script.getId());
        typeScriptExtend.setCellType(cellTye);
        if(Objects.equals(cellTye,CellType.XUDT_COMPATIBLE.getValue())&&script.getHashType()==HashType.TYPE.getCode()){

         }else if(Objects.equals(cellTye,CellType.XUDT.getValue())&&(script.getHashType()==HashType.TYPE.getCode()||script.getHashType()==HashType.DATA1.getCode())){
         }else if(Objects.equals(cellTye,CellType.UDT.getValue())&&(script.getHashType()==HashType.TYPE.getCode()||script.getHashType()==HashType.DATA.getCode())){
            Output output = outputMapper.selectOneByTypeScriptId(script.getId());
            byte[] outputData = getOutputData(output);
            if(outputData!=null&outputData.length>=16){
             }
        }


        typeScriptExtendMapper.insert(typeScriptExtend);
    }




    @Scheduled(cron = "${cron.omigaTask:0 */1 * * * ?}")
    public void omigaTask() {
        RLock lock = redissonClient.getLock("omigaTask");
        Boolean isLock = false;
        try {
            isLock = lock.tryLock(5, 30, TimeUnit.SECONDS);
            log.info("omigaTask start isLock is {}", isLock);
            if (!isLock) {
                return;
            }
            RAtomicLong omigaTaskId = redissonClient.getAtomicLong(OMIGA_TASK_SCRIPT_ID);
            long startId = omigaTaskId.get();

            List<byte[]> codeHashes = new ArrayList<>();
            scriptConfig.getTypeScripts().forEach(typeScript -> {
                if(Objects.equals(typeScript.getCellType(),CellType.OMIGA_INSCRIPTION_INFO.getValue())){
                    codeHashes.add(Numeric.hexStringToByteArray(typeScript.getCodeHash()));
                }
            });

            List<Script> scripts = scriptMapper.getBatchCodeHashesAndId(startId,codeHashes, SPORE_BATCH_SIZE);
            if (scripts.isEmpty()) {
                return;
            }

            Long maxScriptId = scripts.stream().mapToLong(Script::getId).max().orElse(startId);
            for (Script script : scripts) {
                buildOmiga(script);
            }
            omigaTaskId.set(maxScriptId);
        } catch (Exception e) {
            log.error("Error in omigaTask", e);
        } finally {
            if (isLock && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void buildOmiga(Script script){
        try {
            List<Output> outputs = outputMapper.selectByTypeScriptId(script.getId());
            outputs.sort(Comparator.comparing(Output::getId));
            outputs.forEach(output -> {
                //1. 数量不会很大 2. 同一主键下 insert会自动更新
                byte[] outputData = getOutputData(output);
                CkbUtil.OmigaInscriptionBasicInfo omigaInscriptionBasicInfo = CkbUtil.parseOmigaInscriptionInfo(Numeric.toHexString(outputData));
                byte[] udtHash = Numeric.hexStringToByteArray(omigaInscriptionBasicInfo.getUdtHash());
                OmigaInscriptionInfo omigaInscriptionInfo = new OmigaInscriptionInfo();
                omigaInscriptionInfo.setOmigaScriptId(script.getId());
                omigaInscriptionInfo.setOmigaScriptHash(script.getScriptHash());
                omigaInscriptionInfo.setName(omigaInscriptionBasicInfo.getName());
                omigaInscriptionInfo.setSymbol(omigaInscriptionBasicInfo.getSymbol());
                omigaInscriptionInfo.setDecimal(omigaInscriptionBasicInfo.getDecimal());
                omigaInscriptionInfo.setMintLimit(omigaInscriptionBasicInfo.getMintLimit());
                omigaInscriptionInfo.setExpectedSupply(omigaInscriptionBasicInfo.getExpectedSupply());
                omigaInscriptionInfo.setUdtHash(udtHash);
                omigaInscriptionInfo.setMintStatus(omigaInscriptionBasicInfo.getMintStatus());
                omigaInscriptionInfo.setTimestamp(output.getBlockTimestamp());
                omigaInscriptionInfoMapper.insert(omigaInscriptionInfo);
                Script udtScript = scriptMapper.findByScriptHashScript(udtHash);
                if(udtScript!=null){
                    TypeScriptExtend typeScriptExtend = new TypeScriptExtend();
                    typeScriptExtend.setScriptId(udtScript.getId());
                    typeScriptExtend.setUdtHash(udtHash);
                    typeScriptExtend.setSymbol(omigaInscriptionInfo.getSymbol());
                    typeScriptExtend.setDecimal(omigaInscriptionInfo.getDecimal());
                    typeScriptExtend.setName(omigaInscriptionInfo.getName());
                    Script lockScript = scriptMapper.selectById(output.getLockScriptId());
                    if(lockScript!=null){
                        typeScriptExtend.setIssuerAddress(TypeConversionUtil.scriptToAddress(lockScript.getCodeHash(), lockScript.getArgs(), lockScript.getHashType()));
                    }
                    typeScriptExtendMapper.insert(typeScriptExtend);
                }
                dobExtendMapper.flush();
            });
        }catch (Exception e){
            log.error("buildOmiga error",e);
        }

    }

}
