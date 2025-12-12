package com.ckb.explore.worker.entity;

import java.io.Serializable;
import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * @TableName live_cells
 */
@Data
public class LiveCells implements Serializable {


    private Long id;

    private BigInteger capacity;

    private Long lockScriptId;

    private Long typeScriptId;

    private BigInteger occupiedCapacity;

    private Long blockNumber;

    private Long blockTimestamp;

    private byte[] txHash;

    private Long txId;

    private byte[] data;

    private Integer outputIndex;

    private Integer inputIndex;

    private byte[] consumedTxHash;

    private static final long serialVersionUID = 1L;
}