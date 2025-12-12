package com.ckb.explore.worker.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName(value = "dob_code")
public class DobCode {
    private Long dobExtendId;

    private Long dobCodeScriptId;

    private byte[] dobCodeScriptArgs;

}
