package com.ckb.explore.worker.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UdtType {

    SUDT(3),
    M_NFT_TOKEN(6),
    NRC_721_TOKEN(7),
    SPORE_CELL(12),
    OMIGA_INSCRIPTION(14),
    XUDT(15),
    XUDT_COMPATIBLE(17),
    DID_CELL(18),
    SSRI(20);

    private final int code;

    public static UdtType valueOf(int code) {
        for (UdtType type : UdtType.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的UDT类型代码: " + code);
    }

}
