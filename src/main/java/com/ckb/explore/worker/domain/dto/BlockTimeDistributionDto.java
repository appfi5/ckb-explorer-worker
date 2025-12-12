package com.ckb.explore.worker.domain.dto;

import lombok.Data;

@Data
public class BlockTimeDistributionDto {

  private float secondUpperBound;

  private Long blockCount;
}
