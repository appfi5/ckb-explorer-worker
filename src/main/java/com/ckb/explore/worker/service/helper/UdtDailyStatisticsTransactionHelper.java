package com.ckb.explore.worker.service.helper;

import com.ckb.explore.worker.entity.UdtDailyStatistics;
import com.ckb.explore.worker.mapper.UdtDailyStatisticsMapper;
import com.ckb.explore.worker.utils.CollectionUtils;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class UdtDailyStatisticsTransactionHelper {

  @Resource
  private UdtDailyStatisticsMapper udtDailyStatisticsMapper;

  @Transactional(rollbackFor = Exception.class, timeout = 600)
  public void batchInsertOrUpdate(List<UdtDailyStatistics> udtDailyStatisticsList) {
    // 分批次写入，每批 1000 条
    List<List<UdtDailyStatistics>> writeBatches = CollectionUtils.splitList(udtDailyStatisticsList, 1000);
    for (List<UdtDailyStatistics> batch : writeBatches) {
      udtDailyStatisticsMapper.insertOrUpdate(batch);
    }
  }
}
