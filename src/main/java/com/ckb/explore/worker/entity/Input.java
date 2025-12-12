package com.ckb.explore.worker.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * @TableName input
 */
@Data
public class Input implements Serializable {
    private Long id;

    private Long outputId;

    private Object preOutpointTxHash;

    private Integer preOutpointIndex;

    private Object since;

    private Long consumedTxId;

    private Object consumedTxHash;

    private Integer inputIndex;

    private static final long serialVersionUID = 1L;
}