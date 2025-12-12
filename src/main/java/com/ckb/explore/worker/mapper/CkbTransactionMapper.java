package com.ckb.explore.worker.mapper;

import com.ckb.explore.worker.entity.LastNDaysTransactionFeeRates;
import com.ckb.explore.worker.entity.TransactionFeeRates;
import com.ckb.explore.worker.entity.CkbTransaction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import org.apache.ibatis.annotations.Select;

/**
* @author dell
* @description 针对表【ckb_transaction】的数据库操作Mapper
* @createDate 2025-08-22 18:30:37
* @Entity com.ckb.explore.worker.entity.CkbTransaction
*/
public interface CkbTransactionMapper extends BaseMapper<CkbTransaction> {

     List<CkbTransaction> selectByBlockNumberWithJoin(@Param("blockNumber") Long blockNumber );

      @Select("select count(*) from ckb_transaction where block_timestamp >= #{startedAt} and block_timestamp < #{endedAt}")
     Long getTransactionsCountInPeriod(@Param("startedAt") Long startedAt, @Param("endedAt") Long endedAt);

      @Select("select Sum(input_count)  from ckb_transaction WHERE block_timestamp >= #{startedAt} AND block_timestamp <  #{endedAt} And tx_index != 0")
      Long getDeadCellsCountByTime(@Param("startedAt") Long startedAt, @Param("endedAt") Long endedAt);
      
      List<TransactionFeeRates> selectRecentTransactionFeeRates();

      List<LastNDaysTransactionFeeRates> selectLastNDaysTransactionFeeRates(@Param("nDays") int nDays);
}




