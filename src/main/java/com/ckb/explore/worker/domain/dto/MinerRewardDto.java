package com.ckb.explore.worker.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MinerRewardDto {
  private byte[] minerScript;

  /**
   * 矿工当天挖的块数
   */
  private Integer count;

  /**
   * 矿工当天挖的块的奖励
   */
  private Long userReward;
}
