package com.ckb.explore.worker.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * @TableName udt_accounts
 */
@Data
public class UdtAccounts implements Serializable {
    private Long typeScriptId;

    private BigInteger amount;

    private Long lockScriptId;


    private static final long serialVersionUID = 1L;
}