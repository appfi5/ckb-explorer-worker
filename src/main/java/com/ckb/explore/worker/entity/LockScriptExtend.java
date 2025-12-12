package com.ckb.explore.worker.entity;

import lombok.Data;

/**
 * @TableName lock_script_extend
 */
@Data
public class LockScriptExtend {

    private Long scriptId;

    private Integer lockType;
}
