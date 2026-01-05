package com.ckb.explore.worker.domain.dto;

import lombok.Data;

@Data
public class TransactionConfirmationTimeDto {
  private String txHash;

  private String confirmationTime;
}
