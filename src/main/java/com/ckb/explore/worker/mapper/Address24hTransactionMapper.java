package com.ckb.explore.worker.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.ckb.explore.worker.entity.Address24hTransaction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
* @author dell
* @description 针对表【address_24h_transaction】的数据库操作Mapper
* @createDate 2025-09-03 17:37:17
* @Entity com.ckb.explore.worker.entity.Address24hTransaction
*/
@DS("risingwave")
public interface Address24hTransactionMapper extends BaseMapper<Address24hTransaction> {


    @Select("select count(DISTINCT ckb_transaction_id) from address_24h_transaction where block_timestamp >= #{twentyFourHoursAgo}")
    Long getTransactionsLast24hrs(@Param("twentyFourHoursAgo") Long twentyFourHoursAgo);


    Long getMaxTimeByLockScriptId(@Param("lockScriptId") Long lockScriptId,@Param("typeScriptId") Long typeScriptId);



}




