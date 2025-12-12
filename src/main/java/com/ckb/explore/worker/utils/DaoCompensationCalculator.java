package com.ckb.explore.worker.utils;

import com.ckb.explore.worker.domain.dto.DaoCellDto;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * DAO补偿计算器
 * 用于计算DAO存款单元格的未认领补偿金额
 */
@Slf4j
public class DaoCompensationCalculator {

  /**
   * 计算DAO补偿金额
   * @param cellInfo 需计算DAO补偿金额的Cell信息
   * @param withdrawBlockDao 当前块的dao信息
   * @param depositBlockDao 存DAO的块的dao信息
   * @return
   */
  public static BigInteger call(DaoCellDto cellInfo, byte[] withdrawBlockDao, byte[] depositBlockDao) {

      if (cellInfo == null) {
          return BigInteger.ZERO;
      }
      try {
        var compensationGeneratingCapacity = cellInfo.getValue().subtract(cellInfo.getOccupiedCapacity());

        if (depositBlockDao == null || depositBlockDao.length < 32) {
          return BigInteger.ZERO;
        }
        var parsedDepositBlockDao = CkbUtil.parseDao(depositBlockDao);
        if (parsedDepositBlockDao == null) {
          return BigInteger.ZERO;
        }

        if (withdrawBlockDao == null || withdrawBlockDao.length < 32) {
          return BigInteger.ZERO;
        }
        var parsedWithdrawBlockDao = CkbUtil.parseDao(withdrawBlockDao);
        if (parsedWithdrawBlockDao == null) {
          return BigInteger.ZERO;
        }

        return compensationGeneratingCapacity.multiply(parsedWithdrawBlockDao.getArI()).divide(parsedDepositBlockDao.getArI()).subtract(compensationGeneratingCapacity);
      } catch (Exception e) {
          // 记录计算错误，但不抛出异常
          log.error("计算DAO补偿金额出错", e);
      }
      return BigInteger.ZERO;
  }
}