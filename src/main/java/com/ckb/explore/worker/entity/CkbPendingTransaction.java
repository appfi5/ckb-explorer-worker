package com.ckb.explore.worker.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ckb_transaction")
public class CkbPendingTransaction {

  private byte[] txHash;

  private byte[] version;

  private Integer inputCount;

  private Integer outputCount;

  private byte[] witnesses;

  private byte[] headerDeps;

  // 注意交易的bytes跟旧浏览器差了4字节，展示时需要加回去，如果计算费率的话，需要加8字节
  private Long bytes;

  private Integer status;

  private Long createdAt;

  private Long updatedAt;
}