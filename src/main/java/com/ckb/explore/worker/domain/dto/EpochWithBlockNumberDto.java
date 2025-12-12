package com.ckb.explore.worker.domain.dto;

import lombok.Data;

@Data
public class EpochWithBlockNumberDto {

  private Long epochNumber;

  private byte[] difficulty;

  private Long firstBlockNumber;

  private Long lastBlockNumber;

}
