package com.ckb.explore.worker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ckb.explore.worker.domain.dto.BlockDaoDto;
import com.ckb.explore.worker.domain.dto.BlockTimeDistributionDto;
import com.ckb.explore.worker.domain.dto.CommonStatisticInfoDto;
import com.ckb.explore.worker.domain.dto.EpochDto;
import com.ckb.explore.worker.domain.dto.EpochInfoDto;
import com.ckb.explore.worker.domain.dto.EpochWithBlockNumberDto;
import com.ckb.explore.worker.domain.dto.MinerRewardDto;
import com.ckb.explore.worker.entity.Block;
import com.ckb.explore.worker.service.BlockService;
import com.ckb.explore.worker.mapper.BlockMapper;
import com.ckb.explore.worker.utils.CollectionUtils;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * @author dell
 * @description 针对表【block】的数据库操作Service实现
 * @createDate 2025-08-22 18:30:33
 */
@Service
public class BlockServiceImpl extends ServiceImpl<BlockMapper, Block>
    implements BlockService {

  @Override
  public Long getMaxBlockNumber() {
    Long maxBlockNumber = baseMapper.getMaxBlockNumber();
    return maxBlockNumber == null ? 0 : maxBlockNumber;
  }

  @Override
  public Long getCreationBlockTimestamp() {
    return baseMapper.getCreationBlockTimestamp();
  }

  @Override
  public Long getMaxTimestamp() {
    return baseMapper.getMaxTimestamp();
  }

  @Override
  public EpochInfoDto findEpochInfoByTargetEpochNumber(Long epochNumber) {
    return baseMapper.findEpochInfoByTargetEpochNumber(epochNumber);
  }

  @Override
  public Long getMaxEpochNumber() {
    return baseMapper.getMaxEpochNumber();
  }

  @Override
  public byte[] getDaoByBlockNumber(Long blockNumber) {

    LambdaQueryWrapper<Block> queryWrapper = new LambdaQueryWrapper();
    queryWrapper.select(Block::getDao);
    queryWrapper.eq(Block::getBlockNumber, blockNumber);
    var result = baseMapper.selectOne(queryWrapper);
    return result.getDao();
  }

  @Override
  public Map<Long, byte[]> getBlockDaos(Set<Long> blockNumbers) {
    List<List<Long>> batches = CollectionUtils.splitIntoBatches(blockNumbers, 1000);
    Map<Long, byte[]> blockDaos = new HashMap<>(blockNumbers.size()); // 预分配容量
    // 分批查询并合并结果
    for (List<Long> batch : batches) {
      List<BlockDaoDto> batchResult = baseMapper.getBlockDaos(batch);
      if(!batchResult.isEmpty()){
        blockDaos.putAll(batchResult.stream()
            .collect(Collectors.toMap(
                BlockDaoDto::getBlockNumber, // key：blockNumber
                BlockDaoDto::getDao,         // value：dao字段
                (existingValue, newValue) -> newValue, // 若key重复，保留后者
                HashMap::new                 // 指定Map实现（可选，默认是HashMap）
            )));
      }
    }
    return blockDaos;
  }

  @Override
  public CommonStatisticInfoDto getCommonStatisticInfo(Long startAt, Long endAt) {
    return baseMapper.getCommonStatisticInfo(startAt, endAt);
  }

  @Override
  public List<BlockTimeDistributionDto> getBlockTimeDistribution(Long startBlockNumber,
      Long tipBlockNumber) {
    return baseMapper.getBlockTimeDistribution(startBlockNumber,tipBlockNumber);
  }

  @Override
  public BigInteger sumRewardByBlockNumber(Long maxBlockNumber, Long minBlockNumber) {
    return baseMapper.sumRewardByBlockNumber(maxBlockNumber, minBlockNumber);
  }

  @Override
  public List<EpochWithBlockNumberDto> findEpochWithBlockNumberByTime(Long startAt, Long endAt) {
    return baseMapper.findEpochWithBlockNumberByTime(startAt, endAt);
  }

  @Override
  public List<BlockDaoDto> getBlockDaoWithBlockTimestamp(Set<Long> blockNumbers) {
    List<List<Long>> batches = CollectionUtils.splitIntoBatches(blockNumbers, 1000);
    List<BlockDaoDto> blockDaoWithBlockTimestamp = new ArrayList<>(blockNumbers.size()); // 预分配容量
    // 分批查询并合并结果
    for (List<Long> batch : batches) {
      List<BlockDaoDto> batchResult = baseMapper.getBlockDaos(batch);
      if(!batchResult.isEmpty()){
        blockDaoWithBlockTimestamp.addAll(batchResult);
      }
    }
    return blockDaoWithBlockTimestamp;
  }

  @Override
  public BlockDaoDto getBlockDaoDtoByBlockNumber(Long blockNumber) {
    return baseMapper.getBlockDaoDtoByBlockNumber(blockNumber);
  }

  @Override
  public EpochDto getEpochByBlockNumber(Long blockNumber) {
    return baseMapper.getEpochByBlockNumber(blockNumber);
  }

  @Override
  public Long getMaxBlockNumberByTime(Long startAt, Long endAt){
    return baseMapper.getMaxBlockNumberByTime(startAt, endAt);
  }

  @Override
  public List<MinerRewardDto> getMinerWithRewardsByDate(Long startedAt, Long endedAt) {
    return baseMapper.getMinerWithRewardsByDate(startedAt, endedAt);
  }
}




