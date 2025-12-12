package com.ckb.explore.worker.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ckb.explore.worker.config.mybatis.AddressBalanceRankingTypeHandler;
import com.ckb.explore.worker.config.mybatis.TransactionFeeRatesTypeHandler;
import com.ckb.explore.worker.domain.dto.AddressBalanceRankingWrapper;
import com.ckb.explore.worker.domain.dto.TransactionFeeRatesWrapper;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * StatisticInfo 统计信息实体类
 * 对应数据库表：statistic_infos
 */
@Data
@TableName("statistic_infos")
public class StatisticInfo {

  /**
   * 主键ID
   */
  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * 过去24小时的交易数量
   */
  @TableField(value = "transactions_last_24hrs")
  private Long transactionsLast24hrs;

  /**
   * 每分钟交易数量
   */
  private Long transactionsCountPerMinute;

  /**
   * 平均区块时间（秒）
   */
  private Double averageBlockTime;

  /**
   * 哈希率（算力）
   */
  private BigDecimal hashRate;

  /**
   * 区块链基本信息
   */
  private String blockchainInfo;

  /**
   * 地址余额排名（JSON格式）
   */
  @TableField(typeHandler = AddressBalanceRankingTypeHandler.class)
  private AddressBalanceRankingWrapper addressBalanceRanking;

  @TableField(typeHandler = TransactionFeeRatesTypeHandler.class)
  private TransactionFeeRatesWrapper transactionFeeRates;

  private String lastNDaysTransactionFeeRates;

  @TableField(fill = FieldFill.INSERT)
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.UPDATE)
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime updatedAt;
}