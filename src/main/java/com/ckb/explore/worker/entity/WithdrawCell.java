package com.ckb.explore.worker.entity;

import lombok.Data;
import java.math.BigInteger;

/**
 * 提款细胞实体类（对应物化视图 withdraw_cell）
 * 数据来源于 output 表中 type_script_id 匹配且 data 不为 '\x0000000000000000' 的记录
 */
@Data
public class WithdrawCell {

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
   * 交易ID（对应SQL中的 output.tx_id）
   */
  private Long txId;

  /**
   * 交易哈希（对应SQL中的 output.tx_hash）
   */
  private byte[] txHash;

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

  /**
   * 区块时间戳（对应SQL中的 output.block_timestamp，毫秒级）
   */
  private Long blockTimestamp;

  /**
   * 锁定脚本ID（对应SQL中的 output.lock_script_id）
   */
  private Long lockScriptId;

  /**
   * 被消耗的交易哈希（对应SQL中的 output.consumed_tx_hash）
   * 未被消耗时为null
   */
  private byte[] consumedTxHash;

  /**
   * 输入索引（对应SQL中的 output.input_index）
   * 未被消耗时为null
   */
  private Integer inputIndex;

  /**
   * 被消耗的区块号（对应SQL中的 output.consumed_block_number）
   * 未被消耗时为null
   */
  private Long consumedBlockNumber;

  /**
   * 被消耗的区块时间戳（对应SQL中的 output.consumed_timestamp，毫秒级）
   * 未被消耗时为null
   */
  private Long consumedBlockTimestamp;
}
