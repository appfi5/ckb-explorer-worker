package com.ckb.explore.worker.service;

import com.ckb.explore.worker.domain.dto.BlockDaoDto;
import com.ckb.explore.worker.domain.dto.BlockTimeDistributionDto;
import com.ckb.explore.worker.domain.dto.CommonStatisticInfoDto;
import com.ckb.explore.worker.domain.dto.EpochDto;
import com.ckb.explore.worker.domain.dto.EpochInfoDto;
import com.ckb.explore.worker.domain.dto.EpochWithBlockNumberDto;
import com.ckb.explore.worker.domain.dto.MinerRewardDto;
import com.ckb.explore.worker.entity.Block;
import com.baomidou.mybatisplus.extension.service.IService;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.annotations.Param;

/**
* @author dell
* @description 针对表【block】的数据库操作Service
* @createDate 2025-08-22 18:30:33
*/
public interface BlockService extends IService<Block> {

    Long getMaxBlockNumber();

    Long getCreationBlockTimestamp();

    Long getMaxTimestamp();

    EpochInfoDto findEpochInfoByTargetEpochNumber(Long epochNumber);

    Long getMaxEpochNumber();

    byte[] getDaoByBlockNumber(Long blockNumber);

    Map<Long, byte[]> getBlockDaos(Set<Long> blockNumbers);

    CommonStatisticInfoDto getCommonStatisticInfo(Long startAt, Long endAt);

    List<BlockTimeDistributionDto> getBlockTimeDistribution(
        Long startBlockNumber,
        Long tipBlockNumber
    );

    BigInteger sumRewardByBlockNumber(Long maxBlockNumber, Long minBlockNumber);

    List<EpochWithBlockNumberDto> findEpochWithBlockNumberByTime(Long startAt, Long endAt);

    List<BlockDaoDto> getBlockDaoWithBlockTimestamp(Set<Long> blockNumbers);

    BlockDaoDto getBlockDaoDtoByBlockNumber(Long blockNumber);

    EpochDto getEpochByBlockNumber(Long blockNumber);

    Long getMaxBlockNumberByTime(Long startAt, Long endAt);

    List<MinerRewardDto> getMinerWithRewardsByDate(Long startedAt, Long endedAt);
}
