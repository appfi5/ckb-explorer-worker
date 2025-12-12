package com.ckb.explore.worker.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigInteger;
import lombok.Data;

/**
 * dao_contracts 表对应的实体类
 */
@Data
@TableName("dao_contracts")
public class DaoContracts {

  /**
   * 主键 ID
   */
  private Long id;

  /**
   * DAO 总存款
   */
  private BigInteger totalDeposit;

  /**
   * 存款者数量
   */
  private Integer depositorsCount;

  /**
   * 领取的利息（numeric(30,0) ）
   */
  private BigInteger claimedCompensation;

  /**
   * 未领取的利息（numeric(30,0)）
   */
  private BigInteger unclaimedCompensation;
}
