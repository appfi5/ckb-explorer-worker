package com.ckb.explore.worker.entity;

import java.io.Serializable;
import java.util.List;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

/**
 * @TableName ckb_transaction
 */
@Data
public class CkbTransaction implements Serializable {
    private Long id;

    private byte[] txHash;

    private byte[] version;

    private Integer inputCount;

    private Integer outputCount;

    private byte[] witnesses;

    private Long blockId;

    private Long blockNumber;

    private byte[] blockHash;

    private Integer txIndex;

    private byte[] headerDeps;

    private Long cycles;

    private Long transactionFee;

    // 注意交易的bytes跟旧浏览器差了4字节，展示时需要加回去，如果计算费率的话，需要加8字节
    private Long bytes;

    private Long capacityInvolved;

    private Long blockTimestamp;

    @TableField(exist = false) // 非数据库字段
    private List<Output> outputs;

    private static final long serialVersionUID = 1L;
}