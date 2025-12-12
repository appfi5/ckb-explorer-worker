package com.ckb.explore.worker.service.impl;

import static com.ckb.explore.worker.constants.CommonConstantsKey.NO_DATA_HASH;
import static com.ckb.explore.worker.constants.CommonConstantsKey.ZERO_LOCK_CODE_HASH;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ckb.explore.worker.entity.Script;
import com.ckb.explore.worker.service.ScriptService;
import com.ckb.explore.worker.mapper.ScriptMapper;
import com.ckb.explore.worker.utils.TypeConversionUtil;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.nervos.ckb.utils.Numeric;
import org.springframework.stereotype.Service;

/**
* @author dell
* @description 针对表【script】的数据库操作Service实现
* @createDate 2025-08-22 18:30:00
*/
@Service
public class ScriptServiceImpl extends ServiceImpl<ScriptMapper, Script>
    implements ScriptService{

  /**
   * 获取所有黑洞地址的Hash，对应block表里的minerHash
   * @return
   */
  @Override
  public List<byte[]> getZeroLockScripts() {
    List<Script> zeroLockScripts = baseMapper.getZeroLockScripts(Numeric.hexStringToByteArray(ZERO_LOCK_CODE_HASH));
    if (zeroLockScripts == null || zeroLockScripts.isEmpty()) {
      return Collections.emptyList();
    }
    var result = zeroLockScripts.stream().map(script -> TypeConversionUtil.lockScriptToScriptHash(script.getCodeHash(), script.getArgs(), script.getHashType()))
        .collect(Collectors.toList());
    var testNetGenesis = TypeConversionUtil.lockScriptToScriptHash(Numeric.hexStringToByteArray(ZERO_LOCK_CODE_HASH),  Numeric.hexStringToByteArray(NO_DATA_HASH), (short) 1);
    if(!result.contains(testNetGenesis)){
      result.add(testNetGenesis);
    }
    return result;
  }
}




