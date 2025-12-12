package com.ckb.explore.worker.entity;

import java.io.Serializable;
import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @TableName output
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Output  {
    private Long id;

    private Long txId;

    private byte[] txHash;

    private Integer outputIndex;

    private BigInteger capacity;

    private Long lockScriptId;

    private Long typeScriptId;

    private byte[] data;

    private BigInteger occupiedCapacity;

    private Integer isSpent;

    private byte[] consumedTxHash;

    private Integer inputIndex;

    private Long blockNumber;

    private Long blockTimestamp;

    private Integer dataSize;

    private byte[] dataHash;

    private Long consumedBlockNumber;

    private Long consumedTimestamp;


    @TableField(exist = false)
    private Script typeScript;

    @TableField(exist = false)
    private Script lockScript;

}