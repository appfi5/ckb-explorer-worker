package com.ckb.explore.worker.domain.dto;

import java.math.BigInteger;
import lombok.Data;

@Data
public class AddressBalanceRankingDto {

  private Long lockScriptId;

  private BigInteger balance;

}
