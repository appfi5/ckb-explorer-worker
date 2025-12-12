package com.ckb.explore.worker.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * @TableName tx_association_header_dep
 */
@Data
public class TxAssociationHeaderDep implements Serializable {
    private Long id;

    private Long txId;

    private Long blockId;

    private static final long serialVersionUID = 1L;
}