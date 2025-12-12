package com.ckb.explore.worker.mapper;

import com.ckb.explore.worker.domain.dto.ActivityAddressContractDistributionDto;
import com.ckb.explore.worker.domain.dto.CkbHodlWaveDto;
import com.ckb.explore.worker.domain.dto.LockScriptIdWithOccupiedCapacityDto;
import com.ckb.explore.worker.domain.dto.OccupiedCapacityWithHolderCountDto;
import com.ckb.explore.worker.domain.dto.UdtCommonStatisticsDto;
import com.ckb.explore.worker.entity.Output;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author dell
* @description 针对表【output】的数据库操作Mapper
* @createDate 2025-08-22 18:30:27
* @Entity com.ckb.explore.worker.entity.Output
*/
public interface OutputMapper extends BaseMapper<Output> {

     List<Output> selectByTxIdWithJoin(@Param("txId") Long txId);

     CkbHodlWaveDto getCkbHodlWave(@Param("startAt") Long startAt,
      @Param("endAt") Long endAt,
      @Param("threeYearsAgo") Long threeYearsAgo,
      @Param("oneYearAgo") Long oneYearAgo,
      @Param("sixMonthsAgo") Long sixMonthsAgo,
      @Param("threeMonthsAgo") Long threeMonthsAgo,
      @Param("oneMonthAgo") Long oneMonthAgo,
      @Param("oneWeekAgo") Long oneWeekAgo);

     Long getHolderCount(@Param("endAt") Long endAt);

     List<ActivityAddressContractDistributionDto> getActivityAddressContractDistribution(@Param("maxBlockNumber") Long maxBlockNumber,
      @Param("minBlockNumber") Long minBlockNumber);

     @Select("select * from output where type_script_id = #{typeScriptId} order by id desc limit 1")
     Output selectLatestByTypeScriptId(@Param("typeScriptId") Long typeScriptId);

     @Select("SELECT\n"
         + "  COUNT(DISTINCT lock_script_id) AS holder_count,\n"
         + "  SUM(occupied_capacity) AS occupied_capacity\n"
         + "FROM output\n"
         + "  WHERE block_timestamp < #{endedAt}\n"
         + "    AND (consumed_timestamp >= #{endedAt}\n"
         + "    OR consumed_timestamp IS NULL)\n")
    OccupiedCapacityWithHolderCountDto sumOccupiedCapacityWithHolderCountByGeneratedBeforeAndUnconsumedAt(
      @Param("endedAt") Long endedAt);

    List<UdtCommonStatisticsDto> getCommonStatistics(@Param("endedAt") Long endedAt, @Param("udtIds") List<Long> udtIds);

  List<LockScriptIdWithOccupiedCapacityDto> getLockScriptIdWithConsumedOccupiedCapacityByDate(@Param("startAt") Long startAt,
      @Param("endAt") Long endAt);

  List<LockScriptIdWithOccupiedCapacityDto> getLockScriptIdWithAddedOccupiedCapacityByDate(@Param("startAt") Long startAt,
      @Param("endAt") Long endAt);

  @Select("SELECT\n"
      + "  lock_script_id AS lock_script_id,\n"
      + "  SUM(occupied_capacity) AS occupied_capacity\n"
      + "FROM output\n"
      + "  WHERE block_timestamp < #{endedAt}\n"
      + "    AND (consumed_timestamp >= #{endedAt}\n"
      + "    OR consumed_timestamp IS NULL)\n"
      + "GROUP BY lock_script_id")
  List<LockScriptIdWithOccupiedCapacityDto> getHistoryOccupiedCapacityWithHolderCount(
      @Param("endedAt") Long endedAt);


    @Select("select * from output where type_script_id = #{typeScriptId} order by id asc limit 1")
    Output selectFirstByTypeScriptId(@Param("typeScriptId") Long typeScriptId);



    @Select("select * from output where type_script_id = #{typeScriptId}  limit 1")
    Output selectOneByTypeScriptId(@Param("typeScriptId") Long typeScriptId);
}




