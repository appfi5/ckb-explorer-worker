package com.ckb.explore.worker.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * @TableName script
 */
@Data
public class Script implements Serializable {
    private Long id;

    private byte[] codeHash;

    private Short hashType;

    private byte[] args;

    private byte[] scriptHash;

    private Long timestamp;

    private int isTypescript;

    private static final long serialVersionUID = 1L;
}