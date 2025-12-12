package com.ckb.explore.worker.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HashType {


  DATA(0),
  TYPE(1),
  DATA1(2),
  DATA2(4);

  private final int code;

}
