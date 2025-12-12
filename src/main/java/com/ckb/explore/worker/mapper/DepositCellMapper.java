package com.ckb.explore.worker.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ckb.explore.worker.domain.dto.DaoCellDto;
import com.ckb.explore.worker.domain.dto.TotalDepositAndDepositorsCountDto;
import com.ckb.explore.worker.entity.DepositCell;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.math.BigInteger;
import java.util.Set;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @description 针对表【deposit_cell】的数据库操作Mapper
 * @Entity com.ckb.explore.worker.entity.DepositCell
 */
@DS("risingwave")
public interface DepositCellMapper extends BaseMapper<DepositCell> {

  /**
   * 计算DAO合约总存款和存款者数量
   * @return
   */
  @Select("select sum(value) as total_deposit, COUNT(DISTINCT lock_script_id) as depositors_count FROM deposit_cell WHERE consumed_tx_hash is NULL OR consumed_tx_hash = ''")
  TotalDepositAndDepositorsCountDto calTotalDepositAndDepositorsCount();

  @Select("SELECT output_id AS output_id,\n"
      + "value AS value, \n"
      + "data AS data, \n"
      + "occupied_capacity AS occupied_capacity, \n"
      + "output_index AS output_index, \n"
      + "block_number AS block_number,\n"
      + "block_timestamp AS block_timestamp \n"
      + "FROM deposit_cell WHERE consumed_tx_hash is NULL OR consumed_tx_hash = '' \n"
      + "order by block_timestamp asc")
  Page<DaoCellDto> getUnConsumedCells(Page page);

  @Select("SELECT sum(value) AS daily_deposit \n"
      + "FROM deposit_cell \n"
      + "WHERE block_timestamp >= #{startedAt} \n"
      + "AND block_timestamp < #{endedAt} ")
  BigInteger getDailyDaoDeposit(@Param("startedAt") Long startedAt, @Param("endedAt") Long endedAt);

  @Select("SELECT sum(value) AS total_deposit \n"
      + "FROM deposit_cell \n"
      + "WHERE block_timestamp < #{endedAt} ")
  BigInteger totalDeposit(@Param("endedAt") Long endedAt);

  @Select("SELECT DISTINCT lock_script_id \n"
      + "FROM deposit_cell \n"
      + "WHERE block_timestamp < #{startedAt} \n"
      + "AND (consumed_block_timestamp >= #{startedAt} OR consumed_block_timestamp is null)")
  Set<Long> existingDepositors(@Param("startedAt") Long startedAt);

  @Select("SELECT DISTINCT lock_script_id \n"
      + "FROM deposit_cell \n"
      + "WHERE block_timestamp >= #{startedAt} \n"
      + "AND block_timestamp < #{endedAt} \n"
      + "AND (consumed_block_timestamp >= #{endedAt} OR consumed_block_timestamp is null)")
  Set<Long> todayDepositors(@Param("startedAt") Long startedAt, @Param("endedAt") Long endedAt);

  /**
   * 获取DAO合约当前总存款者数量(不包含已经提款的)
   * @param endedAt
   * @return
   */
  @Select("SELECT COUNT(DISTINCT lock_script_id) AS unique_address_count \n"
      + "FROM deposit_cell \n"
      + "WHERE block_timestamp < #{endedAt} \n"
      + "AND (consumed_block_timestamp >= #{endedAt} OR consumed_block_timestamp is null)")
  Long daoDepositorsCount(@Param("endedAt") Long endedAt);

  @Select("SELECT output_id AS output_id,\n"
      + "value AS value,\n"
      + "data AS data,\n"
      + "occupied_capacity AS occupied_capacity,\n"
      + "output_index AS output_index,\n"
      + "block_number AS block_number,\n"
      + "block_timestamp AS block_timestamp \n"
      + "FROM deposit_cell \n"
      + "WHERE block_timestamp < #{endedAt} \n"
      + "AND (consumed_block_timestamp >= #{endedAt} OR consumed_block_timestamp is null) \n"
      + "order by block_timestamp asc")
  Page<DaoCellDto> getUnConsumedCellsByEndTime(Page page,@Param("endedAt") Long endedAt);

  /**
   * 获取DAO合约总存款者数量(包含了已经提款的)
   * @param endedAt
   * @return
   */
  @Select("SELECT COUNT(DISTINCT lock_script_id) AS unique_address_count \n"
      + "FROM deposit_cell \n"
      + "WHERE block_timestamp < #{endedAt} ")
  Long getTotalDepositorsCount(@Param("endedAt") Long endedAt);
}