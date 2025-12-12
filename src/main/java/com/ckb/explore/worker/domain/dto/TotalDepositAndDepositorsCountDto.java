package com.ckb.explore.worker.domain.dto;

import java.math.BigDecimal;
import java.math.BigInteger;
import lombok.Data;

@Data
public class TotalDepositAndDepositorsCountDto {

  /**
   * DAO 总存款
   */
  private BigInteger totalDeposit;

  /**
   * 存款者数量
   */
  private Integer depositorsCount;
}
