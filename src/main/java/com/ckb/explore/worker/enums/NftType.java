package com.ckb.explore.worker.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NftType {
    DOB(0),
    M_NFT(1);
    private final int code;
}
