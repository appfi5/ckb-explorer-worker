package com.ckb.explore.worker.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 地址余额排名实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressBalanceRanking {

  private String address;

  private String balance;

  private int ranking;
}
