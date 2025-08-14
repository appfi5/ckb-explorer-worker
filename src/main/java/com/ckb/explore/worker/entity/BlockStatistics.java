package com.ckb.explore.worker.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * @TableName block_statistics
 */
@Data
public class BlockStatistics implements Serializable {
    private Long id;

    private String difficulty;

    private String hash_rate;

    private String live_cells_count;

    private String dead_cells_count;

    private Long block_number;

    private Date created_at;

    private Date updated_at;

    private Long epoch_number;

    private BigDecimal primary_issuance;

    private BigDecimal secondary_issuance;

    private BigDecimal accumulated_total_deposits;

    private BigDecimal accumulated_rate;

    private BigDecimal unissued_secondary_issuance;

    private BigDecimal total_occupied_capacities;

    private static final long serialVersionUID = 1L;
}