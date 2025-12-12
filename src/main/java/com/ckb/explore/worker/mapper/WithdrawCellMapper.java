package com.ckb.explore.worker.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ckb.explore.worker.domain.dto.DaoCellDto;
import com.ckb.explore.worker.entity.WithdrawCell;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.math.BigInteger;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @description 针对表【withdraw_cell】的数据库操作Mapper
 * @Entity com.ckb.explore.worker.entity.WithdrawCell
 */
@DS("risingwave")
public interface WithdrawCellMapper extends BaseMapper<WithdrawCell> {

    @Select("SELECT output_id AS output_id,\n"
        + "value AS value,\n"
        + "data AS data,\n"
        + "occupied_capacity AS occupied_capacity,\n"
        + "output_index AS output_index,\n"
        + "block_number AS block_number,\n"
        + "block_timestamp AS block_timestamp \n"
        + "FROM withdraw_cell WHERE consumed_tx_hash is NULL OR consumed_tx_hash = '' \n"
        + "order by block_timestamp asc")
    Page<DaoCellDto> getUnConsumedCells(Page page);

    @Select("SELECT output_id AS output_id,\n"
        + "value AS value,\n"
        + "data AS data,\n"
        + "occupied_capacity AS occupied_capacity,\n"
        + "output_index AS output_index,\n"
        + "block_number AS block_number,\n"
        + "block_timestamp AS block_timestamp \n"
        + "FROM withdraw_cell WHERE consumed_block_timestamp > ${laseDayStatisticsTime} \n"
        + "order by block_timestamp asc")
    Page<DaoCellDto> getConsumedCells(Page page, @Param("laseDayStatisticsTime") Long laseDayStatisticsTime);

  @Select("SELECT output_id AS output_id,\n"
      + "value AS value,\n"
      + "data AS data,\n"
      + "occupied_capacity AS occupied_capacity,\n"
      + "output_index AS output_index,\n"
      + "block_number AS block_number,\n"
      + "block_timestamp AS block_timestamp \n"
      + "FROM withdraw_cell WHERE consumed_block_timestamp < ${endTime}")
  List<DaoCellDto> getConsumedCellsByEndTime( @Param("endTime")Long endTime);

  @Select("SELECT output_id AS output_id,\n"
      + "value AS value,\n"
      + "data AS data,\n"
      + "occupied_capacity AS occupied_capacity,\n"
      + "output_index AS output_index,\n"
      + "block_number AS block_number,\n"
      + "block_timestamp AS block_timestamp \n"
      + "FROM withdraw_cell WHERE consumed_block_timestamp >= ${startTime} AND consumed_block_timestamp < ${endTime}")
  List<DaoCellDto> getConsumedCellsByStartTimeEndTime( @Param("startTime")Long startTime, @Param("endTime")Long endTime);

  @Select("SELECT sum(value) AS daily_withdraw \n"
      + "FROM withdraw_cell \n"
      + "WHERE block_timestamp >= #{startedAt} \n"
      + "AND block_timestamp < #{endedAt} ")
  BigInteger getDailyWithdraw(@Param("startedAt") Long startedAt, @Param("endedAt") Long endedAt);

  @Select("SELECT sum(value) AS total_withdraw \n"
      + "FROM withdraw_cell \n"
      + "WHERE block_timestamp < #{endedAt} ")
  BigInteger totalWithdraw(@Param("endedAt") Long endedAt);

  @Select("SELECT output_id AS output_id,\n"
      + "value AS value,\n"
      + "data AS data,\n"
      + "occupied_capacity AS occupied_capacity,\n"
      + "output_index AS output_index,\n"
      + "block_number AS block_number,\n"
      + "block_timestamp AS block_timestamp \n"
      + "FROM withdraw_cell \n"
      + "WHERE block_timestamp < #{endedAt} \n"
      + "AND (consumed_block_timestamp >= #{endedAt} OR consumed_block_timestamp is null) \n"
      + "order by block_timestamp asc")
  Page<DaoCellDto> getUnConsumedCellsByEndTime(Page page, @Param("endedAt") Long endedAt);

}