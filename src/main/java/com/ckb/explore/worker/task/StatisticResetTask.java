package com.ckb.explore.worker.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ckb.explore.worker.domain.dto.AddressBalanceRankingWrapper;
import com.ckb.explore.worker.domain.dto.TransactionFeeRatesWrapper;
import com.ckb.explore.worker.entity.AddressBalanceRanking;
import com.ckb.explore.worker.entity.Block;
import com.ckb.explore.worker.entity.StatisticInfo;
import com.ckb.explore.worker.mapper.BlockMapper;
import com.ckb.explore.worker.service.StatisticAddressService;
import com.ckb.explore.worker.service.StatisticInfoService;
import com.ckb.explore.worker.utils.JsonUtil;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.nervos.ckb.CkbRpcApi;
import org.nervos.ckb.type.BlockchainInfo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * StatisticResetTask 统计信息重置定时任务 用于定期重置各种统计信息
 */
@Component
@Slf4j
public class StatisticResetTask {

  @Resource
  private StatisticInfoService statisticInfoService;

  @Resource
  private RedissonClient redissonClient;

  @Resource
  private BlockMapper blockMapper;

  @Resource
  private StatisticAddressService statisticAddressService;

  @Resource
  private CkbRpcApi ckbRpcApi;

  /**
   * 每10秒重置交易计数、平均区块时间和交易费率
   */
  @Scheduled(fixedRateString = "${statistic.resetCommonStatisticsTime:10000}")
  public void resetCommonStatistics() {
    RLock lock = redissonClient.getLock("resetCommonStatistics");
    boolean isLock = false;
    try {
      isLock = lock.tryLock(5, 60, TimeUnit.SECONDS);
      log.info("resetCommonStatistics start, isLock: {}", isLock);
      if (!isLock) {
        return;
      }
      log.info("reset transactions_count_per_minute, average_block_time");
      resetStatistics("transactions_count_per_minute", "average_block_time","transaction_fee_rates");
    } catch (Exception e) {
      log.error("resetCommonStatistics error: {}", e.getMessage(), e);
    } finally {
      if (isLock && lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  /**
   * 每10分钟重置过去24小时交易数
   */
  @Scheduled(fixedRateString = "${statistic.resetTransactionsLast24hrsTime:600000}")
  public void resetTransactionsLast24Hours() {
    RLock lock = redissonClient.getLock("resetTransactionsLast24hrs");
    boolean isLock = false;
    try {
      isLock = lock.tryLock(5, 60, TimeUnit.SECONDS);
      log.info("reset transactions_last_24hrs start, isLock: {}", isLock);
      if (!isLock) {
        return;
      }
      log.info("reset transactions_last_24hrs");
      resetStatistics("transactions_last_24hrs");
    } catch (Exception e) {
      log.error("reset transactions_last_24hrs error: {}", e.getMessage(), e);
    } finally {
      if (isLock && lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }

  }

  /**
   * 每小时重置哈希率
   */
  @Scheduled(fixedRateString = "${statistic.resetHashRate:3600000}")
  public void resetHashRate() {
    RLock lock = redissonClient.getLock("resetHashRate");
    boolean isLock = false;
    try {
      isLock = lock.tryLock(10, 60, TimeUnit.SECONDS);
      log.info("resetHashRate start, isLock: {}", isLock);
      if (!isLock) {
        return;
      }
      log.info("reset hash_rate");
      resetStatistics("hash_rate", "blockchain_info");
    } catch (Exception e) {
      log.error("resetHashRate error: {}", e.getMessage(), e);
    } finally {
      if (isLock && lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  /**
   * 每4小时重置地址余额排名、矿工排名和过去n天交易费率
   */
  @Scheduled(fixedRateString = "${statistic.resetRankingStatistics:14400000}")
  public void resetRankingStatistics() {
    RLock lock = redissonClient.getLock("resetRankingStatistics");
    boolean isLock = false;
    try {
      isLock = lock.tryLock(10, TimeUnit.SECONDS);
      log.info("resetRankingStatistics start, isLock: {}", isLock);
      if (!isLock) {
        return;
      }
      log.info("reset address_balance_ranking, last_n_days_transaction_fee_rates");
      resetStatistics("address_balance_ranking", "last_n_days_transaction_fee_rates");
    } catch (Exception e) {
      log.error("resetRankingStatistics error: {}", e.getMessage(), e);
    } finally {
      if (isLock && lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  /**
   * 重置统计信息 这里简单实现了重置逻辑，实际应该根据业务需求实现具体的重置逻辑
   */
  private void resetStatistics(String... fieldNames) {
    try {
      RLock createUpdateLock = redissonClient.getLock("createOrUpdateStatisticInfoLock");
      StatisticInfo statisticInfo;
      boolean createLockAcquired= false;
      try {
        // 获取创建更新锁（保证记录唯一创建和更新原子性）
        createLockAcquired = createUpdateLock.tryLock(5, 60, TimeUnit.SECONDS);
        if (!createLockAcquired) {
          log.warn("获取创建更新锁失败，跳过本次更新");
          return;
        }
        // 获取默认的统计信息记录
        LambdaQueryWrapper<StatisticInfo> wrapperStatisticInfo = new LambdaQueryWrapper<StatisticInfo>().orderByDesc(StatisticInfo::getId)
            .last("LIMIT 1");
        statisticInfo = statisticInfoService.getOne(wrapperStatisticInfo);
        if (statisticInfo == null) {
          // 如果不存在默认记录，则创建一个新的
          statisticInfo = new StatisticInfo();
          statisticInfo.setId(1L);
          statisticInfo.setCreatedAt(LocalDateTime.now());
          statisticInfo.setUpdatedAt(LocalDateTime.now());
          statisticInfoService.save(statisticInfo);
          log.info("创建新的 StatisticInfo 记录");
        }
      } finally {
        // 释放创建更新锁
        if (createLockAcquired && createUpdateLock.isHeldByCurrentThread()) {
          createUpdateLock.unlock();
        }
      }

      // 获取最新区块信息
      LambdaQueryWrapper<Block> wrapper = new LambdaQueryWrapper<Block>().orderByDesc(Block::getId)
          .last("LIMIT 1");
      Block tipBlock = blockMapper.selectOne(wrapper);
      var tipBlockNumber = tipBlock.getBlockNumber();
      // 根据字段名重置对应的统计字段
      for (String fieldName : fieldNames) {
        switch (fieldName) {
          case "transactions_count_per_minute":
            statisticInfo.setTransactionsCountPerMinute(statisticInfoService.getTransactionsCountPerMinute(tipBlockNumber));
            break;
          case "average_block_time":
            statisticInfo.setAverageBlockTime(statisticInfoService.getAverageBlockTime(tipBlockNumber, tipBlock.getTimestamp()));
            break;
          case "transactions_last_24hrs":
            statisticInfo.setTransactionsLast24hrs(statisticInfoService.getTransactionsLast24hrs(tipBlock.getTimestamp()));
            break;
          case "hash_rate":
            statisticInfo.setHashRate(statisticInfoService.hashRate(tipBlockNumber));
            break;
          case "blockchain_info":
            BlockchainInfo data = ckbRpcApi.getBlockchainInfo();
            statisticInfo.setBlockchainInfo(JsonUtil.toJSONString(data));
            break;
          case "address_balance_ranking":
            // 获取地址余额排名前50的数据
            List<AddressBalanceRanking> rankingList = statisticAddressService.getAddressBalanceRanking();
            if(!rankingList.isEmpty()){
              statisticInfo.setAddressBalanceRanking(new AddressBalanceRankingWrapper(rankingList));
            }
            break;
          case "transaction_fee_rates":
            var transactionFeeRates = statisticInfoService.getTransactionFeeRates();
            if(!transactionFeeRates.isEmpty()){
              statisticInfo.setTransactionFeeRates(new TransactionFeeRatesWrapper(transactionFeeRates));
            }
            break;
          case "last_n_days_transaction_fee_rates":
            statisticInfo.setLastNDaysTransactionFeeRates(JsonUtil.toJSONString(statisticInfoService.getLastNDaysTransactionFeeRates()));
            break;
          default:
            log.warn("Unknown field to reset: {}", fieldName);
        }
      }

      // 保存更新后的统计信息
      statisticInfoService.saveOrUpdate(statisticInfo);
    } catch (Exception e) {
      log.error("Failed to reset statistics: {}", e.getMessage(), e);
    }
  }
}