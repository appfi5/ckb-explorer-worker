package com.ckb.explore.worker.utils;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.nervos.ckb.Network;
import org.nervos.ckb.type.Script;
import org.nervos.ckb.type.Script.HashType;
import org.nervos.ckb.type.concrete.Byte32Vec;
import org.nervos.ckb.type.concrete.BytesVec;
import org.nervos.ckb.type.concrete.Uint256;
import org.nervos.ckb.utils.Numeric;
import org.nervos.ckb.utils.address.Address;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TypeConversionUtil {

  @Value("${ckb.netWork}")
  private Integer netWork;

  // 静态变量
  private static Integer staticNetWork;

  // 在 Bean 初始化时赋值给静态变量
  @PostConstruct
  public void init() {
    staticNetWork = this.netWork;
  }

  // 提供静态方法访问
  public static Network getNetwork() {
    return staticNetWork == 1  ? Network.MAINNET : Network.TESTNET;
  }

  /**
   * 将 Long 类型转换为 String 类型
   *
   * @param value 要转换的 Long 值
   * @return 转换后的 String 值，如果输入为 null 则返回 null
   */
  public static String longToString(Long value) {
    return value != null ? value.toString() : null;
  }

  /**
   * 将 Integer 类型转换为 String 类型
   *
   * @param value 要转换的 Integer 值
   * @return 转换后的 String 值，如果输入为 null 则返回 null
   */
  public static String integerToString(Integer value) {
    return value != null ? value.toString() : null;
  }

  /**
   * 将 byte[] 类型转换为 String 类型
   *
   * @param value 要转换的字节数组
   * @return 转换后的 String 值，如果输入为 null 则返回 null；去除左边所有的'0'字符后返回
   */
  public static String byteToString(byte[] value) {
    if (value == null) {
      return null;
    }
    var data = Numeric.toHexStringNoPrefix(value);
    // 去除左边所有的'0'字符
    if (data != null && !data.isEmpty()) {
      // 查找第一个非'0'字符的位置
      int firstNonZeroIndex = 0;
      while (firstNonZeroIndex < data.length() && data.charAt(firstNonZeroIndex) == '0') {
        firstNonZeroIndex++;
      }
      // 如果全部是'0'，则返回"0"
      if (firstNonZeroIndex == data.length()) {
        return "0";
      }
      // 否则返回去除左边'0'后的字符串
      return data.substring(firstNonZeroIndex);
    }
    return data;
  }

  /**
   * 将 byte[] 类型的lockScript转换为 String 类型的Address
   *
   * @param value 要转换的 byte[] 值
   * @return 转换后的 String 值，如果输入为 null 则返回 null
   */
  public static String lockScriptToAddress(byte[] value) {
    var net = getNetwork();
    org.nervos.ckb.type.concrete.Script script = org.nervos.ckb.type.concrete.Script.builder(value).build();
    var scriptNew = new Script(script.getCodeHash().getItems(),script.getArgs().getItems(),
        HashType.unpack(script.getHashType()));
    var addrResult = new Address(scriptNew, net);
    return addrResult.encode();
  }

  /**
   * 将 byte[] 类型转换为 String 类型Hash
   *
   * @param value 要转换的 Integer 值
   * @return 转换后的 String 值，如果输入为 null 则返回 null
   */
  public static String byteToStringHash(byte[] value) {
    return value != null ? Numeric.toHexString(value) : null;
  }

  /**
   * 将 byte[] 类型转换为 List<String> 类型Witnesses
   *
   * @param value 要转换的 Integer 值
   * @return 转换后的 String 值，如果输入为 null 则返回 null
   */
  public static List<String> byteToWitnesses(byte[] value) {
    if(value == null) {
      return null;
    }
    var bytesVec = BytesVec.builder(value).build();
    var bytesList = bytesVec.getItems();
    List<String> witnessList = new ArrayList<>();
    for(int i = 0; i < bytesList.length; i++){
      var bytes = bytesList[i];
      witnessList.add(Numeric.toHexString(bytes.getItems()));
    }
    return witnessList;
  }

  /**
   * 将 byte[] 类型转换为 List<String> 类型 哈希列表
   *
   * @param value 要转换的 Integer 值
   * @return 转换后的 String 值，如果输入为 null 则返回 null
   */
  public static List<String> byteToHashList(byte[] value) {
    if(value == null) {
      return null;
    }
    var byte32Vec = Byte32Vec.builder(value).build();
    var byte32List = byte32Vec.getItems();
    List<String> hashs = new ArrayList<>();
    for(int i = 0; i < byte32List.length; i++){
      var bytes = byte32List[i];
      hashs.add(Numeric.toHexString(bytes.getItems()));
    }
    return hashs;
  }

  /**
   * 将 script结构体的lockScript转换为 String 类型的Address
   *
   * @param codeHash
   * @param args
   * @param hashType
   * @return
   */
  public static String scriptToAddress(byte[] codeHash, byte[] args, Short hashType){
    var net = getNetwork();
    Script script = new Script(codeHash,
        args,
        HashType.unpack(hashType.byteValue()));
    return new Address(script, net).encode();
  }

  public static BigInteger byteConvertToUInt256(byte[] bytes)
  {
    if(bytes == null) {
      return BigInteger.ZERO;
    }
    var uint256 = Uint256.builder( bytes).build();
    return new BigInteger(uint256.getItems());
  }

  public static byte[] lockScriptToScriptHash(byte[] codeHash, byte[] args, Short hashType) {
    Script script = new Script(codeHash,
        args,
        HashType.unpack(hashType.byteValue()));

    return script.pack().toByteArray();
  }
}