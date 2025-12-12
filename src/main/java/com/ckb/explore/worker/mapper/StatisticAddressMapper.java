package com.ckb.explore.worker.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.ckb.explore.worker.domain.dto.AddressBalanceDistributionDto;
import com.ckb.explore.worker.domain.dto.AddressBalanceRankingDto;
import com.ckb.explore.worker.entity.StatisticAddress;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigInteger;

/**
 * @author dell
 * @description 针对表【statistic_address】的数据库操作Mapper
 * @createDate 2025-09-02 17:07:22
 * @Entity com.ckb.explore.worker.entity.StatisticAddress
 */
@DS("risingwave")
public interface StatisticAddressMapper extends BaseMapper<StatisticAddress> {

  @Select("select * from statistic_address where lock_script_id = #{lockScriptId}")
  StatisticAddress findByLockScriptId(@Param("lockScriptId") Long lockScriptId);

  List<AddressBalanceRankingDto> getAddressBalanceRanking(@Param("lockScriptIds")List<Long> lockScriptIds);

  List<AddressBalanceDistributionDto> getAddressBalanceDistribution(@Param("lockScriptIds")List<Long> lockScriptIds);
}




