package com.ckb.explore.worker.domain.dto;

import java.math.BigInteger;
import java.util.List;
import lombok.Data;

@Data
public class CommonStatisticInfoDto {

  private Long transactionsCount;

  private Long maxBlockNumber;

  private Long minBlockNumber;

  private Long maxTimestamp;

  private Long minTimestamp;

  private Long totalBlocksCount;

  private Long liveCellsCount;

  private Long totalUnclesCount;

  private BigInteger totalTxFee;

  private Long maxEpochNumber;
}
