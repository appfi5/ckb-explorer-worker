package com.ckb.explore.worker.domain.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MinerRewardInfo {

  private String miner;

  private Integer count;

  private Long userReward;

  private BigDecimal percent;

  private BigDecimal userHashRate;
}
