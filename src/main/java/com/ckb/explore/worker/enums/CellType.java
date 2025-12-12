package com.ckb.explore.worker.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.EnumSet;

@Getter
@AllArgsConstructor
public enum CellType {
    NORMAL(0),
    NERVOS_DAO_DEPOSIT(1),
    NERVOS_DAO_WITHDRAWING(2),
    UDT(3),
    M_NFT_ISSUER(4),
    M_NFT_CLASS(5),
    M_NFT_TOKEN(6),
    NRC_721_TOKEN(7),
    NRC_721_FACTORY(8),
    COTA_REGISTRY(9),
    COTA_REGULAR(10),
    SPORE_CLUSTER(11),
    SPORE_CELL(12),
    OMIGA_INSCRIPTION_INFO(13),
    OMIGA_INSCRIPTION(14),
    XUDT(15),
    UNIQUE_CELL(16),
    XUDT_COMPATIBLE(17),
    DID_CELL(18),
    STABLEPP_POOL(19),
    SSRI(20);
    private  int value;

    public static CellType valueOf(int value) {
        for (CellType type : CellType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的CellType代码: " + value);
    }

    private   static EnumSet<CellType> udtCellType = EnumSet.of(UDT,XUDT,XUDT_COMPATIBLE);

    public   boolean isUdtType(){
        return udtCellType.contains(this);
    }
}
