package com.ckb.explore.worker.domain.dto;

import java.math.BigDecimal;
import java.math.BigInteger;
import lombok.Data;

@Data
public class DaoInterestsSumWithTimeDto {
  private BigInteger daoInterestsSum;

  // 有息存款总额
  private BigInteger interestBearingDeposits;

  // 有息存款时间贡献总和
  private BigDecimal sumInterestBearing;
}
