package com.ckb.explore.worker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;
import com.ckb.explore.worker.config.mybatis.MinerRewardInfoTypeHandler;
import com.ckb.explore.worker.domain.dto.MinerRewardInfoWrapper;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class MinerDailyStatistics implements Serializable {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long createdAtUnixtimestamp;

  private Long maxBlockNumber;

  private Long minBlockNumber;

  // 延迟十一个块的奖励 单位ckb
  private BigDecimal totalReward;

  private String totalHashRate;

  // total_reward * 1_000_000_000.0 / hash_rate
  private String avgRor;

  @TableField(typeHandler = MinerRewardInfoTypeHandler.class)
  private MinerRewardInfoWrapper miners;

  private OffsetDateTime createdAt;

  private OffsetDateTime updatedAt;

  // 版本号：@Version标记为乐观锁字段，更新时自动+1；新增时默认0
  @Version
  private Integer version;

  private static final long serialVersionUID = 1L;
}
