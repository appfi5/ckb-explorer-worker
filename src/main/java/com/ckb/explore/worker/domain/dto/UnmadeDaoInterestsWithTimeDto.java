package com.ckb.explore.worker.domain.dto;

import java.math.BigDecimal;
import java.math.BigInteger;
import lombok.Data;

@Data
public class UnmadeDaoInterestsWithTimeDto {
  // 未认领的DAO补偿总额
  private BigInteger totalUnmadeDaoInterests = BigInteger.ZERO;

  // 无息存款总额
  private BigInteger uninterestBearingDeposits = BigInteger.ZERO;

  // 无息存款时间贡献总和,单位：天
  private BigDecimal sumUninterestBearing = BigDecimal.ZERO;
}
