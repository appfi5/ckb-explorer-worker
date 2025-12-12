package com.ckb.explore.worker.mapper;

import com.ckb.explore.worker.domain.dto.BlockDaoDto;
import com.ckb.explore.worker.domain.dto.BlockTimeDistributionDto;
import com.ckb.explore.worker.domain.dto.CommonStatisticInfoDto;
import com.ckb.explore.worker.domain.dto.EpochDto;
import com.ckb.explore.worker.domain.dto.EpochInfoDto;
import com.ckb.explore.worker.domain.dto.EpochWithBlockNumberDto;
import com.ckb.explore.worker.domain.dto.MinerRewardDto;
import com.ckb.explore.worker.entity.Block;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.math.BigInteger;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author dell
 * @description 针对表【block】的数据库操作Mapper
 * @createDate 2025-08-22 18:30:33
 * @Entity com.ckb.explore.worker.entity.Block
 */
public interface BlockMapper extends BaseMapper<Block> {

  @Select("select max(block_number) from block")
  Long getMaxBlockNumber();

  @Select("select timestamp from block where block_number = 0")
  Long getCreationBlockTimestamp();

  CommonStatisticInfoDto getCommonStatisticInfo(@Param("startAt") Long startAt, @Param("endAt") Long endAt);

  List<EpochWithBlockNumberDto> findEpochWithBlockNumberByTime(@Param("startAt") Long startAt, @Param("endAt") Long endAt);

  @Select("SELECT SUM(reward) FROM block WHERE block_number >= #{minBlockNumber} AND block_number <= #{maxBlockNumber}")
  BigInteger sumRewardByBlockNumber(@Param("maxBlockNumber") Long maxBlockNumber, @Param("minBlockNumber") Long minBlockNumber);

  List<BlockTimeDistributionDto> getBlockTimeDistribution(
      @Param("startBlockNumber") Long startBlockNumber,
      @Param("tipBlockNumber") Long tipBlockNumber
  );

  @Select("select max(timestamp) from block")
  Long getMaxTimestamp();

  EpochInfoDto findEpochInfoByTargetEpochNumber(@Param("epochNumber") Long epochNumber);

  @Select("select max(epoch_number) from block")
  Long getMaxEpochNumber();

  List<BlockDaoDto> getBlockDaos(@Param("blockNumbers") List<Long> blockNumbers);

  @Select(" SELECT block_number ,\n"
      + "           dao,\n"
      + "            timestamp\n"
      + "    FROM block"
      + "    where block_number = #{blockNumber}")
  BlockDaoDto getBlockDaoDtoByBlockNumber(@Param("blockNumber") Long blockNumber);

  @Select("SELECT epoch_number as number, block_number - start_number as index,  epoch_length as length  FROM block WHERE block_number = #{blockNumber}")
  EpochDto getEpochByBlockNumber(@Param("blockNumber") Long blockNumber);

  @Select("SELECT MAX(block_number) FROM block WHERE timestamp >= #{startAt} AND  timestamp < #{endAt} ")
  Long getMaxBlockNumberByTime(@Param("startAt") Long startAt, @Param("endAt") Long endAt);

  List<MinerRewardDto> getMinerWithRewardsByDate(@Param("startAt") Long startAt, @Param("endAt") Long endAt);
}




