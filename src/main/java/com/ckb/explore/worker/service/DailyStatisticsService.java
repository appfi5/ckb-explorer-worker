package com.ckb.explore.worker.service;

import com.ckb.explore.worker.entity.DailyStatistics;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author dell
* @description 针对表【daily_statistics】的数据库操作Service
* @createDate 2025-08-18 17:48:35
*/
public interface DailyStatisticsService extends IService<DailyStatistics> {

  Long getMaxCreatAt();

  Long getMinCreatAt();
  /**
   * 测试验证用
   * @param date
   * @return
   */
  boolean generateDailyStat(Long date);

  void statistic(Long startDate, Long endDate);

  void reSetMiningReward();

  void reSetOccupiedCapacityWithHolderCount();

  DailyStatistics getHashRateByDate(Long  date);

  void reSetTreasuryAmount();
}
