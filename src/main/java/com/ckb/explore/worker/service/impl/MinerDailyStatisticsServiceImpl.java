package com.ckb.explore.worker.service.impl;

import static com.ckb.explore.worker.constants.CommonConstantsKey.SECONDS_IN_DAY;
import static com.ckb.explore.worker.constants.RedisConstantsKey.LOCK_WAIT_TIME;
import static com.ckb.explore.worker.constants.RedisConstantsKey.MINER_LAST_PROCESSED_KEY;
import static com.ckb.explore.worker.constants.RedisConstantsKey.MINER_STATISTIC_DATE_LOCK_KEY;
import static com.ckb.explore.worker.constants.RedisConstantsKey.MINER_STATISTIC_LOCK_KEY;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ckb.explore.worker.domain.dto.MinerRewardDto;
import com.ckb.explore.worker.domain.dto.MinerRewardInfo;
import com.ckb.explore.worker.domain.dto.MinerRewardInfoWrapper;
import com.ckb.explore.worker.entity.DailyStatistics;
import com.ckb.explore.worker.entity.MinerDailyStatistics;
import com.ckb.explore.worker.mapper.MinerDailyStatisticsMapper;
import com.ckb.explore.worker.service.BlockService;
import com.ckb.explore.worker.service.DailyStatisticsService;
import com.ckb.explore.worker.service.MinerDailyStatisticsService;
import com.ckb.explore.worker.utils.AutoReleaseRLock;
import com.ckb.explore.worker.utils.DateUtils;
import com.ckb.explore.worker.utils.TypeConversionUtil;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MinerDailyStatisticsServiceImpl extends
    ServiceImpl<MinerDailyStatisticsMapper, MinerDailyStatistics> implements
    MinerDailyStatisticsService {

  @Resource
  RedissonClient redissonClient;

  @Resource
  BlockService blockService;

  @Resource
  DailyStatisticsService dailyStatisticsService;


  private static final BigDecimal GIGA = BigDecimal.valueOf(1_000_000_000L); // 10^9，用于ROR计算
  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final String AVG_ROR_DEFAULT = "0.0";

  @Override
  public Long getMaxCreatAt(){
    return baseMapper.getMaxCreatAt();
  }

  @Async("asyncStatisticTaskExecutor")
  @Override
  public void perform(Long startDate, Long endDate) {
    // 获取分布式锁，确保只有一个实例执行统计
    RLock lock = redissonClient.getLock(MINER_STATISTIC_LOCK_KEY);

    try(AutoReleaseRLock autoReleaseLock = new AutoReleaseRLock(lock, LOCK_WAIT_TIME, TimeUnit.SECONDS)) {

      log.info("miner统计,开始统计日期范围: [{}, {}]", startDate, endDate);

      // 按日期步进循环处理
      Long currentDate = startDate;

      int reTryCount = 0;
      while (currentDate <= endDate) {
        log.info("miner统计,处理日期: {}", currentDate);

        if (reTryCount > 2) {
          log.error("miner统计,日期 {} 统计3次失败失败，结束统计", currentDate);
          break;
        }

        // 处理单个日期的统计
        boolean success = processSingleDate(currentDate);
        if (success) {
          // 更新最后处理成功的日期
          setLastProcessedDate(currentDate);

          // 处理下一天
          currentDate += SECONDS_IN_DAY;
          reTryCount = 0;
        } else {
          log.error("miner统计,日期 {} 统计失败，重试", currentDate);
          reTryCount ++;
        }
      }
    } catch (Exception e) {
      log.error("miner统计,统计任务执行异常", e);
    }
  }

  public boolean processSingleDate(Long targetDate) {
    RLock dateLock = redissonClient.getLock(MINER_STATISTIC_DATE_LOCK_KEY + targetDate);
    try(AutoReleaseRLock autoReleaseLock = new AutoReleaseRLock(dateLock, LOCK_WAIT_TIME, TimeUnit.SECONDS)) {

      return generateStatistics(targetDate);
    } catch (Exception e) {
      log.error("miner统计，处理日期 {} 发生异常", targetDate, e);
      return false;
    }
  }

  public boolean generateStatistics(Long targetDate) {
    log.info("miner统计,开始处理日期 {}", targetDate);
    Long startedAt = DateUtils.getStartedAt(targetDate); // 获取开始时间 毫秒
    Long endedAt = DateUtils.getEndedAt(targetDate); // 获取结束时间 毫秒

    // 查询目标日期的每日统计是否存在
    DailyStatistics dailyStatistics = dailyStatisticsService.getHashRateByDate(targetDate);
    if (dailyStatistics == null) {
      log.info("miner统计,日期 {} 的每日统计不存在，跳过", targetDate);
      return true;
    }
    var hashRate = dailyStatistics.getAvgHashRate();
    var maxBlockNumber =dailyStatistics.getMaxBlockNumber();

    // 查询当天的miner统计是否存在
    MinerDailyStatistics minerDailyStatistics = baseMapper.getByDate(targetDate);
    if(minerDailyStatistics == null){
      log.info("miner统计,生成日期 {} 的统计数据", targetDate);
      minerDailyStatistics = new MinerDailyStatistics();
      minerDailyStatistics.setCreatedAtUnixtimestamp(targetDate);
      minerDailyStatistics.setCreatedAt(OffsetDateTime.now());
    } else{
      log.info("miner统计,更新日期 {} 的统计数据", targetDate);
    }

    minerDailyStatistics.setUpdatedAt(OffsetDateTime.now());
    minerDailyStatistics.setMaxBlockNumber(maxBlockNumber);
    minerDailyStatistics.setTotalHashRate(hashRate);

    // 查询矿工挖矿数据
    var minerWithRewards = blockService.getMinerWithRewardsByDate(startedAt, endedAt);
    Optional<List<MinerRewardDto>> rewardsOpt = Optional.ofNullable(minerWithRewards);
    MinerDailyStatistics finalMinerDailyStatistics = minerDailyStatistics;
    rewardsOpt.filter(rewards -> !rewards.isEmpty())
        .ifPresentOrElse(
            // 非空场景：计算统计数据
            rewards -> {
              // 计算总块数和总奖励
              int totalBlockCount = rewards.stream()
                  .mapToInt(MinerRewardDto::getCount)
                  .sum();
              long totalRewardLong = rewards.stream()
                  .mapToLong(MinerRewardDto::getUserReward)
                  .sum();
              // 单位CKB
              BigDecimal totalReward = BigDecimal.valueOf(totalRewardLong);

              // 设置块号范围（原逻辑不变）
              finalMinerDailyStatistics.setMinBlockNumber(maxBlockNumber - totalBlockCount + 1);
              finalMinerDailyStatistics.setTotalReward(totalReward);

              // 计算平均ROR（优化hashRate判断，避免重复创建BigDecimal）
              BigDecimal avgRor = calculateAvgRor(totalReward, hashRate);
              finalMinerDailyStatistics.setAvgRor(avgRor.toString());

              // 转换矿工奖励信息（优化lambda可读性，提取局部变量）
              List<MinerRewardInfo> minerRewardInfos = rewards.stream()
                  .map(rewardDto -> convertToMinerRewardInfo(rewardDto, totalBlockCount, hashRate))
                  .collect(Collectors.toList());
              finalMinerDailyStatistics.setMiners(new MinerRewardInfoWrapper(minerRewardInfos));
            },
            // 空数据场景：设置默认值（原逻辑不变，优化空列表处理）
            () -> {
              log.info("miner统计,日期 {} 的挖矿数据不存在", targetDate);
              finalMinerDailyStatistics.setMinBlockNumber(maxBlockNumber);
              finalMinerDailyStatistics.setTotalReward(ZERO);
              finalMinerDailyStatistics.setAvgRor(AVG_ROR_DEFAULT);
              finalMinerDailyStatistics.setMiners(new MinerRewardInfoWrapper(Collections.emptyList()));
            }
        );

    baseMapper.insertOrUpdate(finalMinerDailyStatistics);
    log.info("miner统计,日期 {} 的统计数据生成/更新完成", targetDate);
    return true;
  }

  /**
   * 设置最后处理成功的日期
   */
  private void setLastProcessedDate(Long date) {
    RBucket<String> bucket = redissonClient.getBucket(MINER_LAST_PROCESSED_KEY);
    bucket.set(date.toString());
  }

  /**
   * 计算平均ROR
   */
  private BigDecimal calculateAvgRor(BigDecimal totalReward, String hashRate) {
    // 优化：先判断hashRate是否有效，再转换为BigDecimal（避免重复new）
    if (hashRate == null || ZERO.compareTo(new BigDecimal(hashRate)) >= 0) {
      return ZERO;
    }
    BigDecimal hashRateBig = new BigDecimal(hashRate);
    // 原逻辑：total_reward换算成ckb * 1e9 / hash_rate，保留15位小数，四舍五入 单位 ckb/s
    return totalReward.divide(new BigDecimal("10").pow(8), 8, RoundingMode.DOWN).multiply(GIGA)
        .divide(hashRateBig, 15, RoundingMode.HALF_UP);
  }

  /**
   * 转换MinerRewardDto为MinerRewardInfo
   */
  private MinerRewardInfo convertToMinerRewardInfo(MinerRewardDto rewardDto, int totalBlockCount,String hashRate) {
    // 原逻辑不变，提取局部变量让代码更清晰
    String minerAddress = TypeConversionUtil.lockScriptToAddress(rewardDto.getMinerScript());
    int blockCount = rewardDto.getCount();
    long userReward = rewardDto.getUserReward();

    // 计算占比（原逻辑：count / totalBlockCount，保留16位小数）
    BigDecimal percent = BigDecimal.valueOf(blockCount)
        .divide(BigDecimal.valueOf(totalBlockCount), 16, RoundingMode.HALF_UP);

    // 计算算力占比（原逻辑：percent * hashRate）
    BigDecimal minerHashRate = hashRate != null ? percent.multiply(new BigDecimal(hashRate)) : ZERO;

    return new MinerRewardInfo(minerAddress, blockCount, userReward, percent, minerHashRate);
  }

}
