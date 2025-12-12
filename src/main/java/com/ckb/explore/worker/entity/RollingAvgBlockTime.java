package com.ckb.explore.worker.entity;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class RollingAvgBlockTime {
  private Long timestamp; // 时间戳字段（对应默认排序字段）
  private BigDecimal avgBlockTimeDaily; // 滚动平均区块时间（核心统计字段）
  private BigDecimal avgBlockTimeWeekly;
}
