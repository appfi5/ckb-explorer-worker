package com.ckb.explore.worker.domain.dto;

import lombok.Data;

@Data
public class EpochInfoDto {

  private byte[] difficulty;

  private Integer epochLength;

  private Long minTimestamp;

  private Long maxTimestamp;

  private Long unclesCount;

  private Integer blocksCount;

  private byte[] largestTxHash;

  private Long largestTxBytes;

  private Long maxTxCycles;

  private Long maxBlockCycles;

  private Long largestBlockNumber;

  private Long largestBlockSize;
}
