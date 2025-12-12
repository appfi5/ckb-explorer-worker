package com.ckb.explore.worker.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ckb.explore.worker.entity.UdtAccounts;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigInteger;

/**
* @author dell
* @description 针对表【udt_accounts】的数据库操作Mapper
* @createDate 2025-08-29 15:02:45
* @Entity com.ckb.explore.worker.entity.UdtAccounts
*/
@DS("risingwave")
public interface UdtAccountsMapper extends BaseMapper<UdtAccounts> {

     UdtAccounts findUdtAccountByTypeScriptIdAndLockScriptId(@Param("typeScriptId") Long typeScriptId,@Param("lockScriptId") Long lockScriptId );




}




