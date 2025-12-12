package com.ckb.explore.worker.domain.dto;

import lombok.Data;

@Data
public class UdtCommonStatisticsDto {

  private Long scriptId;

  private Integer holdersCount;

  private Integer ckbTransactionsCount;
}
