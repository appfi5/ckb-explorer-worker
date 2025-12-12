package com.ckb.explore.worker.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("type_script_extend")
public class TypeScriptExtend {

  private Long scriptId;

  private Integer cellType;
}
