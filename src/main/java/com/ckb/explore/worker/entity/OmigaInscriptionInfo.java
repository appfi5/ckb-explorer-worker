package com.ckb.explore.worker.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigInteger;

@Data
@TableName(autoResultMap = true)
public class OmigaInscriptionInfo {
    private Long omigaScriptId;

    private byte[] omigaScriptHash;

    private Integer decimal;

    private String name;

    private String symbol;

    private BigInteger expectedSupply;

    private BigInteger mintLimit;

    private Integer mintStatus;

    private byte[] udtHash;

    private Long timestamp;

}
