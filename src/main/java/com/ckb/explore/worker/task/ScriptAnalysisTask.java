package com.ckb.explore.worker.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ckb.explore.worker.config.ScriptConfig;
import com.ckb.explore.worker.entity.*;
import com.ckb.explore.worker.enums.CellType;
import com.ckb.explore.worker.enums.HashType;
import com.ckb.explore.worker.enums.LockType;
import com.ckb.explore.worker.mapper.*;
import com.ckb.explore.worker.utils.ByteUtils;
import com.ckb.explore.worker.utils.CkbUtil;
import com.ckb.explore.worker.utils.TypeConversionUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.nervos.ckb.utils.Numeric;
import org.nervos.ckb.utils.address.Address;
import org.redisson.api.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    private static final int BATCH_SIZE = 1000;

    private static final int SPORE_BATCH_SIZE = 100;



    private static final String SPORE_TASK_SCRIPT_ID = "SPORE_TASK_SCRIPT_ID";

    private static final String ERROR_TASK_SCRIPT_ID = "ERROR_TASK_SCRIPT_ID";

    private static final String SCRIPT_TASK_SCRIPT_ID = "SCRIPT_TASK_SCRIPT_ID";

    private static final Pattern CHINESE_ONLY_PATTERN = Pattern.compile("^[\\u4E00-\\u9FFF]+$");
    private static final Pattern VISIBLE_CHAR_PATTERN = Pattern.compile("^[\\x21-\\x7E\\u4E00-\\u9FFF]+(?:\\s[\\x21-\\x7E\\u4E00-\\u9FFF]+)*$");

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

            List<Script> scripts = scriptMapper.getBatchSporeById(startId,codeHashes, SPORE_BATCH_SIZE);
            if (scripts.isEmpty()) {
                return;
            }

            Long maxScriptId = scripts.stream().mapToLong(Script::getId).max().orElse(startId);
            for (Script script : scripts) {

                dealDob(script);
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
            RSet<Long> errorTaskScriptIds = redissonClient.getSet(ERROR_TASK_SCRIPT_ID);
            if (errorTaskScriptIds.isEmpty()) {
                return;
            }
            errorTaskScriptIds.forEach(errorTaskScriptId -> {
                Script script = scriptMapper.selectById(errorTaskScriptId);
                errorTaskScriptIds.remove(errorTaskScriptId);
                dealDob(script);
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


    private void dealDob(Script script) {
        RSet<Long> errorTaskScriptId = redissonClient.getSet(ERROR_TASK_SCRIPT_ID);
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
            if ( Objects.equals(typeScript.getCellType(),CellType.SPORE_CLUSTER.getValue()) &&  HashType.DATA1.getCode() == hashType) {
                //sporeCluster 中args为clusterId,非集群模式的统一在初始化sql里处理
                if (script.getArgs() == null || script.getArgs().length == 0) {
                    return;
                }
                //处理sporeCluster
                Output output = outputMapper.selectOneByTypeScriptId(script.getId());
                if(output==null){
                    errorTaskScriptId.add(script.getId());
                    return;
                }

                CkbUtil.SporeClusterData sporeClusterData = CkbUtil.parseSporeClusterData(Numeric.toHexString(getOutputData(output)));

                DobExtend dobExtend = new DobExtend();
                dobExtend.setId(script.getId());
                dobExtend.setDobScriptId(script.getId());
                dobExtend.setArgs(script.getArgs());
                dobExtend.setDobScriptHash(script.getScriptHash());
                dobExtend.setName(sporeClusterData.getName());
                dobExtend.setDescription(sporeClusterData.getDescription());
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
            } else if ((Objects.equals(typeScript.getCellType(),CellType.SPORE_CELL.getValue()) && HashType.DATA1.getCode() == hashType) || (Objects.equals(typeScript.getCellType(),CellType.DID_CELL.getValue()) && HashType.TYPE.getCode() == hashType)) {
                     // 处理sporeNft
                    DobCode dobCode = new DobCode();
                    dobCode.setDobCodeScriptId(script.getId());
                    dobCode.setDobCodeScriptArgs(script.getArgs());
                    Output output = outputMapper.selectOneByTypeScriptId(script.getId());
                    if(output==null){
                        errorTaskScriptId.add(script.getId());
                        return;
                    }
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
                        TypeScriptExtend typeScriptExtend = new TypeScriptExtend();
                        typeScriptExtend.setScriptId(script.getId());
                        typeScriptExtend.setCellType(typeScript.getCellType());
                        typeScriptExtendMapper.insert(typeScriptExtend);
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


}
