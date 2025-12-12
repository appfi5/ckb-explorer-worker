package com.ckb.explore.worker.service;

public interface MinerDailyStatisticsService {

  Long getMaxCreatAt();

  void perform(Long startDate, Long endDate);
}
