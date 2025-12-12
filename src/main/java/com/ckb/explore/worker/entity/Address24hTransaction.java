package com.ckb.explore.worker.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName address_24h_transaction
 */
@Data
@TableName("address_24h_transaction")
public class Address24hTransaction implements Serializable {


    private Long lockScriptId;

    private Long ckbTransactionId;

    private Long blockTimestamp;

    private Long blockNumber;

    private Long typeScriptId;

    private static final long serialVersionUID = 1L;
}