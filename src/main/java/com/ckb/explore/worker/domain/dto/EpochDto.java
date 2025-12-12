package com.ckb.explore.worker.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EpochDto {

  public long number;
  public int index;
  public int length;
}
