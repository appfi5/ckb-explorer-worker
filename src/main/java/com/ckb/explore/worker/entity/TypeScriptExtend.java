package com.ckb.explore.worker.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.ArrayTypeHandler;

@Data
@TableName("type_script_extend")
public class TypeScriptExtend {

  private Long scriptId;

  private Integer cellType;

  private byte[] udtHash;

  private String symbol;

  private String name;

  private Integer decimal;

  private String description;

  private String iconFile;

  private String operatorWebsite;

  private String issuerAddress;

  private Long createdAt;

}
