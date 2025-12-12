package com.ckb.explore.worker.domain.dto;

import lombok.Data;

@Data
public class EpochTimeDistributionDto {
  private Integer rangeUpper;

  private Integer epochCount;
}
