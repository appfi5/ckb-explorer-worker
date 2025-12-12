package com.ckb.explore.worker.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
@AllArgsConstructor
public enum LockType {

  SECP256K1Blake160(1, "SECP256K1/blake160"),
  SECP256k1Multisig5c5069eb(2, "SECP256k1/Multisig(@5c5069eb)"),
  SECP256k1Multisig36c971b8(3, "SECP256k1/Multisig(@36c971b8)"),
  iCKBLogic(4, "iCKB Logic"),
  AnyoneCanPay(5,"AnyoneCanPay"),
  CHEQUE(6,"CHEQUE"),
  JoyId(7,"JoyId"),
  OMNILockV2(8,"OMNI Lock V2"),
  OMNILockV1(9,"OMNI Lock V1"),
  PWLock(10,"PW Lock"),
  Nostr(11,"Nostr"),
  RgbppLock(12,"RgbppLock"),
  BtcTimeLock(13,"BtcTimeLock"),
  AlwaysSuccess(14,"AlwaysSuccess"),
  InputTypeProxyLock(15,"InputTypeProxyLock"),
  OutputTypeProxyLock(16,"OutputTypeProxyLock"),
  LockProxyLock(17,"LockProxyLock"),
  SingleUseLock(18,"Single Use Lock"),
  TypeBurnLock(19,"TypeBurnLock"),
  TimeLock(20,"TimeLock");

  private final int code;
  private final String value;

  public static String getValueByCode(Integer code){
    if(code == null){
      return null;
    }
    LockType[] hashTypes = LockType.values();
    for (LockType hashType : hashTypes) {
      if(hashType.getCode() == code.intValue()){
        return hashType.getValue();
      }
    }
    return "unknown";
  }

  public static LockType getByCode(Integer code){
    if(code == null){
      return null;
    }
    LockType[] hashTypes = LockType.values();
    for (LockType hashType : hashTypes) {
      if(hashType.getCode() == code.intValue()){
        return hashType;
      }
    }
    return null;
  }

  public  static Integer getCodeByValue(String value){
     if(StringUtils.hasLength(value)){
       LockType[] hashTypes = LockType.values();
       for (LockType hashType : hashTypes) {
         if(hashType.getValue().equals(value)){
           return hashType.getCode();
         }
       }
     }
     return null;
  }
}
