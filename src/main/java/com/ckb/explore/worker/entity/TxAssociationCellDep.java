package com.ckb.explore.worker.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * @TableName tx_association_cell_dep
 */
@Data
public class TxAssociationCellDep implements Serializable {
    private Long id;

    private Long txId;

    private Integer index;

    private Object outpointTxHash;

    private Integer outpointIndex;

    private Long outputId;

    private Integer depType;

    private static final long serialVersionUID = 1L;
}