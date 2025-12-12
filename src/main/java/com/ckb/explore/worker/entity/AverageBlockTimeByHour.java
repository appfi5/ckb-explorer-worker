package com.ckb.explore.worker.entity;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class AverageBlockTimeByHour {
  private Long hour; // 时间字段（对应默认排序字段）
  private BigDecimal avgBlockTimePerHour; // 滚动平均区块时间（核心统计字段）
}
