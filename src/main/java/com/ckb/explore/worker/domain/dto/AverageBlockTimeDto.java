package com.ckb.explore.worker.domain.dto;

import com.ckb.explore.worker.entity.RollingAvgBlockTime;
import java.util.List;
import lombok.Data;

@Data
public class AverageBlockTimeDto {

  private Long id;

  private String type;

  private List<RollingAvgBlockTime> averageBlockTime;
}
