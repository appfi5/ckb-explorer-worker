package com.ckb.explore.worker.domain.dto;

import java.math.BigInteger;
import lombok.Data;

@Data
public class DaoCellDto {

  /**
   * 输出ID（对应SQL中的 output.id）
   */
  private Long outputId;

  /**
   * 容量值（对应SQL中的 output.capacity）
   * 区块链中通常为大整数（如CKB的shannon单位），用BigInteger避免溢出
   */
  private BigInteger value;

  /**
   * 数据字段（对应SQL中的 output.data）
   * 此处特征：不等于 E'\\x0000000000000000'（与DepositCell的data字段区分）
   */
  private byte[] data;

  /**
   * 占用容量（对应SQL中的 output.occupied_capacity）
   */
  private BigInteger occupiedCapacity;

  /**
   * 细胞索引（对应SQL中的 output.output_index）
   */
  private Integer outputIndex;

  /**
   * 区块号（对应SQL中的 output.block_number）
   */
  private Long blockNumber;

  private Long blockTimestamp;
}
