package com.ckb.explore.worker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ckb.explore.worker.entity.LastNDaysTransactionFeeRates;
import com.ckb.explore.worker.entity.TransactionFeeRates;
import com.ckb.explore.worker.entity.StatisticInfo;
import java.math.BigDecimal;
import java.util.List;

/**
 * StatisticInfoService 统计信息服务接口
 * 用于操作statistic_infos表相关的业务逻辑
 */
public interface StatisticInfoService extends IService<StatisticInfo> {

  BigDecimal hashRate(Long tipBlockNumber);

  double getAverageBlockTime(Long tipBlockNumber, Long timestamp);

  Long getTransactionsLast24hrs(Long timestamp);

  Long getTransactionsCountPerMinute(Long tipBlockNumber);

  List<TransactionFeeRates> getTransactionFeeRates();

  List<LastNDaysTransactionFeeRates> getLastNDaysTransactionFeeRates();
}