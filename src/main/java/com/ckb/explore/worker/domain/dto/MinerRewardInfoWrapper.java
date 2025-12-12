package com.ckb.explore.worker.domain.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MinerRewardInfoWrapper {
  List<MinerRewardInfo> data;
}
