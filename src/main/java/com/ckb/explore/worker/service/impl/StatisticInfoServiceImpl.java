package com.ckb.explore.worker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ckb.explore.worker.domain.dto.TransactionConfirmationTimeDto;
import com.ckb.explore.worker.entity.LastNDaysTransactionFeeRates;
import com.ckb.explore.worker.entity.TransactionFeeRates;
import com.ckb.explore.worker.entity.Block;
import com.ckb.explore.worker.entity.StatisticInfo;
import com.ckb.explore.worker.entity.UncleBlock;
import com.ckb.explore.worker.mapper.Address24hTransactionMapper;
import com.ckb.explore.worker.mapper.BlockMapper;
import com.ckb.explore.worker.mapper.CkbPendingTransactionMapper;
import com.ckb.explore.worker.mapper.StatisticInfoMapper;
import com.ckb.explore.worker.mapper.UncleBlockMapper;
import com.ckb.explore.worker.mapper.CkbTransactionMapper;
import com.ckb.explore.worker.service.StatisticInfoService;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.nervos.ckb.utils.Numeric;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * StatisticInfoServiceImpl 统计信息服务实现类
 * 实现statistic_infos表相关的业务逻辑
 */
@Service
public class StatisticInfoServiceImpl extends ServiceImpl<StatisticInfoMapper, StatisticInfo> implements
    StatisticInfoService {

  @Value("${statistic.hashRateStatisticalInterval:100}")
  private int hashRateStatisticalInterval;

  @Value("${statistic.averageBlockTimeInterval:100}")
  private int averageBlockTimeInterval;

  @Resource
  private BlockMapper blockMapper;

  @Resource
  private UncleBlockMapper uncleBlockMapper;

  @Resource
  private Address24hTransactionMapper address24hTransactionMapper;

  @Resource
  private CkbTransactionMapper ckbTransactionMapper;

  @Resource
  private CkbPendingTransactionMapper ckbPendingTransactionMapper;

  @Override
  public BigDecimal hashRate(Long tipBlockNumber){
    // 查询指定区块号之前的指定数量的区块
    LambdaQueryWrapper<Block> blockQueryWrapper = new LambdaQueryWrapper<>();
    blockQueryWrapper.select(Block::getId, Block::getTimestamp, Block::getDifficulty)
        .le(Block::getBlockNumber, tipBlockNumber)
        .orderByDesc(Block::getId)
        .last("LIMIT " + hashRateStatisticalInterval);

    List<Block> blocks = blockMapper.selectList(blockQueryWrapper);

    // 如果没有找到区块，返回0
    if (blocks == null || blocks.isEmpty()) {
      return new BigDecimal("0");
    }

    // 计算所有区块的难度总和
    BigDecimal totalDifficulties = BigDecimal.ZERO;
    for (Block block : blocks) {
      if (block.getDifficulty() != null) {
        totalDifficulties = totalDifficulties.add( new BigDecimal(Numeric.toBigInt(block.getDifficulty())));
      }
    }

    // 加上这些区块对应的所有叔块的难度总和
    LambdaQueryWrapper<UncleBlock> uncleBlockQueryWrapper = new LambdaQueryWrapper<>();
    uncleBlockQueryWrapper.select(UncleBlock::getDifficulty)
        .in(UncleBlock::getBlockNumber, blocks.stream().map(Block::getBlockNumber).toArray());

    List<UncleBlock> uncleBlocks = uncleBlockMapper.selectList(uncleBlockQueryWrapper);
    for (UncleBlock uncleBlock : uncleBlocks) {
      if (uncleBlock.getDifficulty() != null) {
        totalDifficulties = totalDifficulties.add(new BigDecimal(Numeric.toBigInt(uncleBlock.getDifficulty())));
      }
    }

    // 计算时间差（最新区块的时间减去最旧区块的时间）
    // 由于我们是按降序排列的，所以blocks.get(0)是最新的区块，blocks.get(blocks.size() - 1)是最旧的区块
    long totalTime = blocks.get(0).getTimestamp() - blocks.get(blocks.size() - 1).getTimestamp();

    // 如果时间差为0，返回0
    if (totalTime <= 0) {
      return new BigDecimal("0");
    }

    // 计算哈希率（难度总和除以时间差），并保留6位小数
    BigDecimal hashRateValue = totalDifficulties.divide(new BigDecimal(totalTime), 6, RoundingMode.HALF_UP);

    return hashRateValue;
  }

  @Override
  public double getAverageBlockTime(Long tipBlockNumber, Long timestamp){

    // 计算起始块号，确保不小于0
    Long startBlockNumber = Math.max(tipBlockNumber - averageBlockTimeInterval + 1, 0L);

    // 只查询起始块的时间戳
    LambdaQueryWrapper<Block> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.select(Block::getTimestamp)
        .eq(Block::getBlockNumber, startBlockNumber);

    Block startBlock = blockMapper.selectOne(queryWrapper);

    // 如果没有找到起始块，返回默认值
    if (startBlock == null || startBlock.getTimestamp() == null) {
      return 0.0;
    }

    // 计算总块时间（用入参timestamp减去查到的startBlock的时间）
    double totalBlockTime = timestamp - startBlock.getTimestamp(); // 毫秒级

    // 计算块的数量
    int blocksCount = blocksCount(tipBlockNumber);

    // 返回平均块时间
    return totalBlockTime / blocksCount;
  }

  /**
   * 计算块的数量
   */
  private int blocksCount(Long tipBlockNumber) {
    return tipBlockNumber > averageBlockTimeInterval ? averageBlockTimeInterval : tipBlockNumber.intValue();
  }

  public Long getTransactionsLast24hrs(Long timestamp) {
    // 改成从24小时历史交易表读取
    // 计算24小时前的时间戳（24小时 = 24 * 60 * 60 * 1000毫秒）
    long twentyFourHoursAgo = timestamp - 24 * 60 * 60 * 1000L;

    return address24hTransactionMapper.getTransactionsLast24hrs(twentyFourHoursAgo);
  }

  public Long getTransactionsCountPerMinute(Long tipBlockNumber) {
    // 定义区间为100
    int interval = 100;

    // 计算起始块号，确保不小于0
    Long startBlockNumber = Math.max(tipBlockNumber - interval + 1, 0L);

    // 查询起始块和结束块的时间戳
    LambdaQueryWrapper<Block> timestampQueryWrapper = new LambdaQueryWrapper<>();
    timestampQueryWrapper.select(Block::getTimestamp)
        .in(Block::getBlockNumber, startBlockNumber, tipBlockNumber);

    List<Block> timestampBlocks = blockMapper.selectList(timestampQueryWrapper);

    // 如果没有找到区块，返回0
    if (timestampBlocks == null || timestampBlocks.isEmpty()) {
      return 0L;
    }

    // 获取时间戳列表
    List<Long> timestamps = timestampBlocks.stream()
        .map(Block::getTimestamp)
        .filter(t -> t != null)
        .toList();

    // 如果时间戳列表为空，返回0
    if (timestamps.isEmpty()) {
      return 0L;
    }

    // 查询区间内的所有区块的交易总数，使用selectObjs方法获取交易数
    LambdaQueryWrapper<Block> transactionsQueryWrapper = new LambdaQueryWrapper<>();
    transactionsQueryWrapper.select(Block::getTransactionsCount)
        .between(Block::getBlockNumber, startBlockNumber, tipBlockNumber);

    List<Object> transactionsCounts = blockMapper.selectObjs(transactionsQueryWrapper);

    // 使用stream API计算总交易数
    long transactionsCount = transactionsCounts.stream()
        .filter(count -> count != null)
        .mapToLong(count -> ((Number) count).longValue())
        .sum();

    // 计算总块时间差（单位：毫秒）
    long totalBlockTime = timestamps.stream().max(Long::compareTo).orElse(0L) -
        timestamps.stream().min(Long::compareTo).orElse(0L);

    // 如果时间差为0，返回0
    if (totalBlockTime <= 0) {
      return 0L;
    }

    // 计算每分钟交易数（转换为分钟：/1000/60），保留3位小数
    double transactionsPerMinute = transactionsCount * 60000.0 / totalBlockTime;

    // 返回结果（截断到3位小数后转换为Long）
    return Math.round(transactionsPerMinute * 1000) / 1000L;
  }

  @Override
  public List<TransactionFeeRates> getTransactionFeeRates() {

    // 从pg库查最新的一万条交易
    List<TransactionFeeRates> transactions = ckbTransactionMapper.selectRecentTransactionFeeRates();

    // 转成以hash为key的map
    Map<String, TransactionFeeRates> transactionMap = transactions.stream()
        .collect(Collectors.toMap(TransactionFeeRates::getTxHash, transaction -> transaction,
            (oldVal, newVal) -> newVal, () -> new HashMap<>(transactions.size())));
    // 从pending库查最新确认的一万条交易
    List<TransactionConfirmationTimeDto> transactionConfirmationTimes = ckbPendingTransactionMapper.selectTransactionConfirmationTimes();

    List<TransactionFeeRates> result = transactionConfirmationTimes.stream()
        .map(transactionConfirmationTime -> {
          String txHash = transactionConfirmationTime.getTxHash();
          var transaction = transactionMap.get(txHash);
          if (transaction == null) {
            // 交易不存在时返回null，后续过滤
            return null;
          }
          return new TransactionFeeRates(
              transaction.getId(),
              transaction.getTxHash(),
              transaction.getFeeRate(),
              transaction.getTimestamp(),
              transactionConfirmationTime.getConfirmationTime());
        }).filter(tx -> tx != null && tx.getId() != null)
        .sorted(Comparator.comparing(TransactionFeeRates::getId))
        .collect(Collectors.toList());

    return result;
  }

  @Override
  public List<LastNDaysTransactionFeeRates> getLastNDaysTransactionFeeRates() {
    // 默认最近20天
    return ckbTransactionMapper.selectLastNDaysTransactionFeeRates(20);
  }
}
