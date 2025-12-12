package com.ckb.explore.worker.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * @TableName block
 */
@Data
public class Block implements Serializable {
    private Long id;

    private byte[] blockHash;

    private Long blockNumber;

    private byte[] compactTarget;

    private byte[] parentHash;

    private byte[] nonce;

    private byte[] difficulty;

    private Long timestamp;

    private byte[] version;

    private byte[] transactionsRoot;

    private Integer transactionsCount;

    private byte[] epoch;

    private Long startNumber;

    private Integer epochLength;

    private Long epochNumber;

    private byte[] dao;

    private byte[] proposalsHash;

    private byte[] extraHash;

    private byte[] extension;

    private byte[] proposals;

    private Integer proposalsCount;

    private Integer unclesCount;

    private byte[] uncleBlockHashes;

    private byte[] minerScript;

    private String minerMessage;

    private Long reward;

    private Long totalTransactionFee;

    private Long cellConsumed;

    private Long totalCellCapacity;

    private Integer blockSize;

    private Long cycles;

    private Integer liveCellChanges;

    private Long blockInterval;

    private static final long serialVersionUID = 1L;
}