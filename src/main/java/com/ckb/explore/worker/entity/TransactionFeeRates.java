package com.ckb.explore.worker.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionFeeRates {
  private String id;
  private String feeRate;
  private String timestamp;
  private String confirmationTime;
}
