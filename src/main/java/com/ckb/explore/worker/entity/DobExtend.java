package com.ckb.explore.worker.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.ArrayTypeHandler;


@Data
@TableName(autoResultMap = true)
public class DobExtend {

    private Long id;

    private Long dobScriptId;

    private byte[] dobCodeHash;

    private byte[] dobScriptHash;

    private Long blockTimestamp;

    private String name;

    private String description;




    @TableField(typeHandler = ArrayTypeHandler.class)
    private String[] tags;

    private byte[] args;

    private Long lockScriptId;

    private String creator;




}
