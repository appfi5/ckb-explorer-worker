package com.ckb.explore.worker.domain.dto;

import lombok.Data;

@Data
public class BlockDaoDto {

  private Long blockNumber;

  private byte[] dao;

  private Long timestamp;
}
