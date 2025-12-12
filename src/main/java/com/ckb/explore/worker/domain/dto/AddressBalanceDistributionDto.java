package com.ckb.explore.worker.domain.dto;

import java.math.BigInteger;
import lombok.Data;

@Data
public class AddressBalanceDistributionDto {

  private BigInteger rangeUpper;

  private BigInteger addressCount;

  private BigInteger cumulativeCount;
}
