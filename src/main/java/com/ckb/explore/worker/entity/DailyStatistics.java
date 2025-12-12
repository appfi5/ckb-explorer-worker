package com.ckb.explore.worker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ckb.explore.worker.config.mybatis.ListStringTypeHandler;
import com.ckb.explore.worker.config.mybatis.MapTypeHandler;
import com.ckb.explore.worker.domain.dto.ListStringWrapper;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.Data;

/**
 * @TableName daily_statistics
 */
@Data
@TableName(value = "daily_statistics")
public class DailyStatistics implements Serializable {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long transactionsCount; // 1

  private Long addressesCount;// 1

  private String totalDaoDeposit;// dao

  private Long maxBlockNumber;

  private Long blockTimestamp;

  private Long createdAtUnixtimestamp;// 1

  private OffsetDateTime createdAt;

  private OffsetDateTime updatedAt;

  private String daoDepositorsCount;// dao

  private String unclaimedCompensation; // dao phase1_dao_interests + unmade_dao_interests

  private String claimedCompensation; // dao

  private String averageDepositTime; // dao

  private String estimatedApc;

  private String miningReward;// 1

  private String depositCompensation;// dao unclaimed_compensation.to_i + claimed_compensation.to_i

  private String treasuryAmount;// 2 dao  burnt:treasury_amount.to_i + MarketData::BURN_QUOTA

  private String liveCellsCount;// 1

  private String deadCellsCount;// 1

  private String avgHashRate;// 1

  private String avgDifficulty;// 1

  private String uncleRate;// 1

  private String totalDepositorsCount;// dao

  private BigInteger totalTxFee;// 1

  @TableField(typeHandler = ListStringTypeHandler.class)
  private ListStringWrapper addressBalanceDistribution; // 1

  private BigInteger occupiedCapacity;

  private BigInteger dailyDaoDeposit;// dao

  private Integer dailyDaoDepositorsCount;// dao

  private BigInteger dailyDaoWithdraw;// dao

  private BigDecimal circulationRatio;// dao

  private BigInteger totalSupply;// dao

  private BigDecimal circulatingSupply; // dao

  @TableField(typeHandler = ListStringTypeHandler.class)
  private ListStringWrapper blockTimeDistribution; // 1

  @TableField(typeHandler = ListStringTypeHandler.class)
  private ListStringWrapper epochTimeDistribution; // 1

  private Object averageBlockTime; // 在别的表查

  private Object nodesDistribution;

  private Integer nodesCount;

  private BigInteger lockedCapacity;// dao market_data.ecosystem_locked +
//  market_data.team_locked +
//  market_data.private_sale_locked +
//  market_data.founding_partners_locked +
//  market_data.foundation_reserve_locked +†
//  market_data.bug_bounty_locked

  @TableField(typeHandler = MapTypeHandler.class)
  private Map<String, String> ckbHodlWave; // 1

  private Long holderCount; // 1

  private BigInteger knowledgeSize;// dao

  @TableField(typeHandler = MapTypeHandler.class)
  private Map<String, String> activityAddressContractDistribution;// 1

  // 版本号：@Version标记为乐观锁字段，更新时自动+1；新增时默认0
  @Version
  private Integer version;

  private static final long serialVersionUID = 1L;
}