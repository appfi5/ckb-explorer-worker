package com.ckb.explore.worker.service.impl;

import static com.ckb.explore.worker.constants.CommonConstantsKey.ZERO_LOCK_CODE_HASH;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ckb.explore.worker.domain.dto.AddressBalanceRankingDto;
import com.ckb.explore.worker.entity.AddressBalanceRanking;
import com.ckb.explore.worker.entity.Script;
import com.ckb.explore.worker.entity.StatisticAddress;
import com.ckb.explore.worker.mapper.ScriptMapper;
import com.ckb.explore.worker.service.StatisticAddressService;
import com.ckb.explore.worker.mapper.StatisticAddressMapper;
import com.ckb.explore.worker.utils.TypeConversionUtil;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.nervos.ckb.utils.Numeric;
import org.springframework.stereotype.Service;

/**
 * @author dell
 * @description 针对表【statistic_address】的数据库操作Service实现
 * @createDate 2025-09-02 17:07:22
 */
@Service
public class StatisticAddressServiceImpl extends
    ServiceImpl<StatisticAddressMapper, StatisticAddress>
    implements StatisticAddressService {

  @Resource
  private ScriptMapper scriptMapper;

  @Override
  public List<AddressBalanceRanking> getAddressBalanceRanking() {
    var zeroLockScriptIds = scriptMapper.getZeroLockScriptId(Numeric.hexStringToByteArray(ZERO_LOCK_CODE_HASH));
    List<AddressBalanceRankingDto> dbRankingList = baseMapper.getAddressBalanceRanking(zeroLockScriptIds);
    if (dbRankingList.isEmpty()) {
      return Collections.emptyList();
    }
    List<Long> lockScriptIds = dbRankingList.stream()
        .map(AddressBalanceRankingDto::getLockScriptId)
        .toList();
    Map<Long, Script> validScriptMap = scriptMapper.selectByIds(lockScriptIds).stream()
        .collect(Collectors.toMap(Script::getId, Function.identity()));

    // 按余额从高到低排序，设值Ranking
    List<AddressBalanceRankingDto> sortedValidList = dbRankingList.stream()
        // 按余额降序排序
        .sorted(Comparator.comparing(
            item -> item.getBalance(),
            Comparator.reverseOrder() // 降序
        ))
        .collect(Collectors.toList());

    // 转换为最终的返回对象，并计算排名
    return IntStream.range(0, sortedValidList.size())
        .mapToObj(index -> {
          AddressBalanceRankingDto item = sortedValidList.get(index);
          Script script = validScriptMap.get(item.getLockScriptId());

          // 转换地址（封装为工具方法，代码更简洁）
          String address = convertToAddress(script);

          // 排名 = 索引 + 1（动态计算，避免依赖数据库排名字段）
          return new AddressBalanceRanking(address, String.valueOf(item.getBalance()), index + 1);
        })
        .collect(Collectors.toList());

  }

  /**
   * 辅助方法：将 Script 转换为地址（封装重复逻辑，提高可读性）
   *
   * @param script 脚本对象
   * @return 转换后的地址（若 script 为 null，返回空字符串，避免空指针）
   */
  private String convertToAddress(Script script) {
    if (script == null) {
      return "";
    }
    return TypeConversionUtil.scriptToAddress(
        script.getCodeHash(),
        script.getArgs(),
        script.getHashType()
    );
  }
}




