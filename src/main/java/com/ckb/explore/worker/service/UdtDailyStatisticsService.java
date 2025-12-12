package com.ckb.explore.worker.service;

import com.ckb.explore.worker.entity.UdtDailyStatistics;
import java.util.List;

public interface UdtDailyStatisticsService {

  Long getMaxCreatAt();

  boolean processSingleDate(Long targetDate);

  void perform(Long startDate, Long endDate);
}
