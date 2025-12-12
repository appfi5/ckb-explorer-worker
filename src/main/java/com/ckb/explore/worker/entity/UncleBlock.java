package com.ckb.explore.worker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("uncle_block")
public class UncleBlock {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Integer index;

  private byte[] blockHash;

  private Long blockNumber;

  private byte[] compactTarget;

  private byte[] parentHash;

  private byte[] nonce;

  private byte[] difficulty;

  private Long timestamp;

  private byte[] version;

  private byte[] transactionsRoot;

  private byte[] epoch;

  private byte[] dao;

  private byte[] proposalsHash;

  private byte[] extraHash;

  private byte[] extension;

  private byte[] proposals;
}