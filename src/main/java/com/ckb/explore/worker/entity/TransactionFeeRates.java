package com.ckb.explore.worker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionFeeRates {
  private String id;
  @JsonIgnore
  private String txHash;
  private String feeRate;
  // 单位秒
  private String timestamp;
  private String confirmationTime;
}
