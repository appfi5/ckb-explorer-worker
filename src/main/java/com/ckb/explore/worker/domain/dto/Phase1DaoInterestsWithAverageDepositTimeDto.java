package com.ckb.explore.worker.domain.dto;

import java.math.BigDecimal;
import java.math.BigInteger;
import lombok.Data;

@Data
public class Phase1DaoInterestsWithAverageDepositTimeDto {

  // phase1Dao的利息总额
  private BigInteger totalPhase1DaoInterests = BigInteger.ZERO;

  // 有息存款总额
  private BigInteger interestBearingDeposits = BigInteger.ZERO;

  // 有息存款时间贡献总和,单位：天
  private BigDecimal sumInterestBearing = BigDecimal.ZERO;
}
