package com.ckb.explore.worker.service.impl;

import static com.ckb.explore.worker.constants.CommonConstantsKey.BURN_QUOTA;
import static com.ckb.explore.worker.constants.CommonConstantsKey.MILLISECONDS_IN_DAY;
import static com.ckb.explore.worker.constants.CommonConstantsKey.SECONDARY_EPOCH_REWARD;
import static com.ckb.explore.worker.constants.CommonConstantsKey.SECONDS_IN_DAY;
import static com.ckb.explore.worker.constants.CommonConstantsKey.SHANNON_TO_CKB;
import static com.ckb.explore.worker.constants.CommonConstantsKey.ZERO_LOCK_CODE_HASH;
import static com.ckb.explore.worker.constants.RedisConstantsKey.DAILY_LAST_PROCESSED_KEY;
import static com.ckb.explore.worker.constants.RedisConstantsKey.DAILY_STATISTIC_DATE_LOCK_KEY;
import static com.ckb.explore.worker.constants.RedisConstantsKey.DAILY_STATISTIC_LOCK_KEY;
import static com.ckb.explore.worker.constants.RedisConstantsKey.DAILY_STATISTIC_RESET_LOCK_KEY;
import static com.ckb.explore.worker.constants.RedisConstantsKey.LOCK_WAIT_TIME;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ckb.explore.worker.config.ScriptConfig;
import com.ckb.explore.worker.domain.dto.ActivityAddressContractDistributionDto;
import com.ckb.explore.worker.domain.dto.BlockDaoDto;
import com.ckb.explore.worker.domain.dto.CkbHodlWaveDto;
import com.ckb.explore.worker.domain.dto.CommonStatisticInfoDto;
import com.ckb.explore.worker.domain.dto.DaoCellDto;
import com.ckb.explore.worker.domain.dto.DaoInterestsSumWithTimeDto;
import com.ckb.explore.worker.domain.dto.EpochDto;
import com.ckb.explore.worker.domain.dto.EpochWithBlockNumberDto;
import com.ckb.explore.worker.domain.dto.ListStringWrapper;
import com.ckb.explore.worker.domain.dto.LockScriptIdWithOccupiedCapacityDto;
import com.ckb.explore.worker.domain.dto.MarketDataDto;
import com.ckb.explore.worker.domain.dto.OccupiedCapacityWithHolderCountDto;
import com.ckb.explore.worker.domain.dto.Phase1DaoInterestsWithAverageDepositTimeDto;
import com.ckb.explore.worker.domain.dto.UnmadeDaoInterestsWithTimeDto;
import com.ckb.explore.worker.entity.DailyStatistics;
import com.ckb.explore.worker.mapper.CkbTransactionMapper;
import com.ckb.explore.worker.mapper.DepositCellMapper;
import com.ckb.explore.worker.mapper.EpochStatisticMapper;
import com.ckb.explore.worker.mapper.OutputMapper;
import com.ckb.explore.worker.mapper.ScriptMapper;
import com.ckb.explore.worker.mapper.StatisticAddressMapper;
import com.ckb.explore.worker.mapper.WithdrawCellMapper;
import com.ckb.explore.worker.service.BlockService;
import com.ckb.explore.worker.service.DailyStatisticsService;
import com.ckb.explore.worker.mapper.DailyStatisticsMapper;
import com.ckb.explore.worker.service.MarketDataService;
import com.ckb.explore.worker.utils.AutoReleaseRLock;
import com.ckb.explore.worker.utils.CkbUtil;
import com.ckb.explore.worker.utils.CkbUtil.DaoDto;
import com.ckb.explore.worker.utils.DaoCompensationCalculator;
import com.ckb.explore.worker.utils.DateUtils;
import com.ckb.explore.worker.utils.JsonUtil;
import com.ckb.explore.worker.utils.TypeConversionUtil;
import jakarta.annotation.Resource;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.nervos.ckb.utils.Numeric;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
* @author dell
* @description 针对表【daily_statistics】的数据库操作Service实现
* @createDate 2025-08-18 17:48:35
*/
@Service
@Slf4j
public class DailyStatisticsServiceImpl extends ServiceImpl<DailyStatisticsMapper, DailyStatistics>
    implements DailyStatisticsService {

  // 当前数据量测试网100W，主网50W不到，先用ConcurrentHashMap，如果后期增长到千万用户，再调整内存配置或者分片存储或者Redis（String + 高效序列化）
  private final Map<Long, ConcurrentHashMap<Long, BigInteger>> lockScriptIdWithOccupiedCapacityCache = new ConcurrentHashMap<>();

  @Resource
  private RedissonClient redissonClient;

  @Resource
  private CkbTransactionMapper ckbTransactionMapper;

  @Resource
  private BlockService blockService;

  @Resource
  private StatisticAddressMapper statisticAddressMapper;

  @Resource
  private OutputMapper outputMapper;

  @Resource
  private ScriptMapper scriptMapper;

  @Resource
  private EpochStatisticMapper epochStatisticMapper;

  @Resource
  private ScriptConfig scriptConfig;

  @Resource
  private WithdrawCellMapper withdrawCellMapper;

  @Resource
  private DepositCellMapper depositCellMapper;

  @Resource
  private MarketDataService marketDataService;

  @Value("${dao.pageSize:1000}")
  private int daoPageSize;

  @Override
  public Long getMaxCreatAt(){
      return baseMapper.getMaxCreatAt();
  }

  @Override
  public Long getMinCreatAt(){
    return baseMapper.getMinCreatAt();
  }

  /**
   * 获取最后处理成功的日期时间戳
   */
  private Long getLastProcessedDate() {
    RBucket<String> bucket = redissonClient.getBucket(DAILY_LAST_PROCESSED_KEY);
    return bucket.get() == null ? null : Long.parseLong(bucket.get());
  }

  /**
   * 设置最后处理成功的日期
   */
  private void setLastProcessedDate(Long date) {
    RBucket<String> bucket = redissonClient.getBucket(DAILY_LAST_PROCESSED_KEY);
    bucket.set(date.toString());
  }

  /**
   * 获取最后存储的LockScriptIdWithOccupiedCapacity
   */
  private Map<Long, BigInteger> getLastLockScriptIdWithOccupiedCapacity(Long date) {
    ConcurrentHashMap<Long, BigInteger> cache = lockScriptIdWithOccupiedCapacityCache.get(date);
    var result = cache != null ? new HashMap<>(cache) : new HashMap<Long, BigInteger>();
    log.info("读取缓存完成，key: {}，总条数: {}", date, result.size());
    return result;
  }

  private void setLastLockScriptIdWithOccupiedCapacity(Map<Long, BigInteger>  map, Long date) {
    Long yesterday = date - SECONDS_IN_DAY;
    // 删除昨天的缓存
    lockScriptIdWithOccupiedCapacityCache.remove(yesterday);

    lockScriptIdWithOccupiedCapacityCache.put(date, new ConcurrentHashMap<>(map));
    log.info("已保存当天缓存，date: {}，数据量: {}", date, map.size());
  }

  /**
   * 核心统计方法 - 使用日期步进循环替代队列异步处理
   * @param startDate 开始日期（秒）
   * @param endDate 结束日期（秒）
   * @return 是否全部处理成功
   */
  @Async("asyncStatisticTaskExecutor")
  @Override
  public void statistic(Long startDate, Long endDate) {
    // 获取分布式锁，确保只有一个实例执行统计
    RLock lock = redissonClient.getLock(DAILY_STATISTIC_LOCK_KEY);
    try(AutoReleaseRLock autoReleaseRLock = new AutoReleaseRLock(lock, LOCK_WAIT_TIME, TimeUnit.SECONDS)) {

      log.info("开始统计日期范围: [{}, {})", startDate, endDate);
      
      // 按日期步进循环处理
      Long currentDate = startDate;

      int reTryCount = 0;
      while (currentDate < endDate) {
        log.info("处理日期: {}", currentDate);

        if (reTryCount > 2) {
          log.error("日期 {} 统计3次失败失败，结束统计", currentDate);
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
          log.error("日期 {} 统计失败，重试", currentDate);
          reTryCount ++;
        }
      }
    } catch (Exception e) {
      log.error("统计任务执行异常", e);
    }
  }
  
  /**
   * 处理单个日期的统计
   * @param date 日期（秒）
   * @return 是否成功
   */
  private boolean processSingleDate(Long date) {
    // 单个日期的分布式锁，防止并发处理同一日期
    RLock dateLock = redissonClient.getLock(DAILY_STATISTIC_DATE_LOCK_KEY + date);
    
    try(AutoReleaseRLock autoReleaseRLock = new AutoReleaseRLock(dateLock, LOCK_WAIT_TIME, TimeUnit.SECONDS)) {

      // 调用核心生成方法
      return  generateDailyStat(date);

    } catch (Exception e) {
      log.error("处理日期 {} 发生异常", date, e);
      return false;
    }
  }


  // 核心生成方法（指定日期统计数据存在则更新，否则新增）
  @Override
  public boolean generateDailyStat(Long date) {
    log.info("开始生成日期 {} 的统计数据", date);
    Long startedAt = DateUtils.getStartedAt(date); // 获取开始时间 毫秒
    Long endedAt = DateUtils.getEndedAt(date); // 获取结束时间 毫秒
    DailyStatistics yesterdayStat = getYesterdayStatistic(date);
    DailyStatistics stat = baseMapper.findDailyStatistic(date);
    if(stat == null){
      stat = new DailyStatistics();
      stat.setCreatedAtUnixtimestamp(date);
    }

    stat.setUpdatedAt(OffsetDateTime.now());
    try {
      // 基础指标计算 查询时间范围【startedAt，endedAt）
      var commonStatisticInfo = calculateCommonStatisticInfo(startedAt, endedAt);
      log.info("日期 {} 查询基础指标结果 {}", date, JsonUtil.toJSONString(commonStatisticInfo));
      // 如果当天没有交易没有出块，则返回昨天的统计数据或默认值
      if(commonStatisticInfo == null || commonStatisticInfo.getTotalBlocksCount() == 0){
        log.info("日期 {} 没有区块数据，使用前一天数据或默认值", date);
        stat.setTransactionsCount(0L);// 当日的交易数
        stat.setBlockTimestamp(yesterdayStat == null? 0L :yesterdayStat.getBlockTimestamp());
        stat.setMaxBlockNumber(yesterdayStat == null? 0L :yesterdayStat.getMaxBlockNumber());
        stat.setAddressesCount(yesterdayStat == null? 0L :yesterdayStat.getAddressesCount());// 历史增量的地址数
        stat.setLiveCellsCount(yesterdayStat == null? "0" :yesterdayStat.getLiveCellsCount());// 历史增量的LiveCells数
        stat.setDeadCellsCount(yesterdayStat == null? "0" :yesterdayStat.getDeadCellsCount());// 历史增量的DeadCells数
        stat.setAvgHashRate("0");// 当天的平均哈希率
        stat.setAvgDifficulty("0.000000");// 当天的平均难度
        stat.setUncleRate("0.000000");// 当天的叔块率
        stat.setAddressBalanceDistribution(new ListStringWrapper(calculateAddressBalanceDistribution()));
        stat.setTotalTxFee( BigInteger.ZERO);// 当天的总手续费
        stat.setMiningReward(yesterdayStat == null? "0":yesterdayStat.getMiningReward());// 累计的挖矿奖励
        stat.setCkbHodlWave(calculateCkbHodlWave(startedAt, endedAt));// ckb分布，跟当天无关
        stat.setHolderCount(yesterdayStat == null? 0: yesterdayStat.getHolderCount());// 累计的HODL数
        stat.setActivityAddressContractDistribution(new HashMap<>());
        stat.setBlockTimeDistribution(yesterdayStat == null? new ListStringWrapper():yesterdayStat.getBlockTimeDistribution());
        stat.setEpochTimeDistribution(new ListStringWrapper(calculateEpochTimeDistribution()));

        // DAO数据相关
        stat.setDailyDaoDeposit(BigInteger.ZERO);
        stat.setTotalDaoDeposit(yesterdayStat == null? "0": yesterdayStat.getTotalDaoDeposit());
        stat.setDaoDepositorsCount(yesterdayStat == null? "0": yesterdayStat.getDaoDepositorsCount());
        stat.setUnclaimedCompensation(yesterdayStat == null? "0": yesterdayStat.getUnclaimedCompensation());
        stat.setClaimedCompensation(yesterdayStat == null? "0": yesterdayStat.getClaimedCompensation());
        stat.setDepositCompensation(yesterdayStat == null? "0": yesterdayStat.getDepositCompensation());
        stat.setAverageDepositTime(yesterdayStat == null? "0": yesterdayStat.getAverageDepositTime());
        stat.setTreasuryAmount(yesterdayStat == null? "0": yesterdayStat.getTreasuryAmount());
        stat.setTotalDepositorsCount(yesterdayStat == null? "0": yesterdayStat.getTotalDepositorsCount());
        stat.setDailyDaoDepositorsCount(0);
        stat.setCirculatingSupply(yesterdayStat == null? BigDecimal.valueOf(0.0): yesterdayStat.getCirculatingSupply());
        stat.setCirculationRatio(yesterdayStat == null? BigDecimal.valueOf(0.0): yesterdayStat.getCirculationRatio());
        stat.setLockedCapacity(yesterdayStat == null? BigInteger.ZERO: yesterdayStat.getLockedCapacity());
        stat.setKnowledgeSize(yesterdayStat == null? BigInteger.ZERO: yesterdayStat.getKnowledgeSize());
        stat.setOccupiedCapacity(yesterdayStat == null? BigInteger.ZERO: yesterdayStat.getOccupiedCapacity());
        baseMapper.insertOrUpdate(stat);
        log.info("日期 {} 的统计数据（无区块）保存完成", date);
        return true;
      }
      stat.setBlockTimestamp(commonStatisticInfo.getMaxTimestamp());
      stat.setMaxBlockNumber(commonStatisticInfo.getMaxBlockNumber());
      var transactionsCount = commonStatisticInfo.getTransactionsCount();
      stat.setTransactionsCount(transactionsCount);// 当天的交易数
      stat.setAddressesCount(calculateAddressesCount(startedAt, endedAt, yesterdayStat));// 历史累加的地址数
      stat.setLiveCellsCount(calculateLiveCellsCount(commonStatisticInfo.getLiveCellsCount(), yesterdayStat).toString());
      stat.setDeadCellsCount(calculateDeadCellsCount(startedAt, endedAt, yesterdayStat).toString());

      // 高级指标计算
      var totalDifficulty = calculateTotalDifficultiesForDay(startedAt, endedAt);
      stat.setAvgHashRate(calculateAvgHashRate(totalDifficulty, commonStatisticInfo.getMaxTimestamp() - commonStatisticInfo.getMinTimestamp()).toString());
      stat.setAvgDifficulty(calculateAvgDifficulty(totalDifficulty,commonStatisticInfo.getTotalBlocksCount()).toString());
      stat.setUncleRate(calculateUncleRate(commonStatisticInfo.getTotalUnclesCount(), commonStatisticInfo.getTotalBlocksCount()).toString());

      stat.setAddressBalanceDistribution(new ListStringWrapper(calculateAddressBalanceDistribution()));
      stat.setTotalTxFee(commonStatisticInfo.getTotalTxFee());

      // DAO数据相关
      // 当天存款数
      log.info("开始计算日期 {} 的DAO相关数据", date);
      BigInteger dailyDaoDeposit = calculateDailyDaoDeposit(startedAt, endedAt);
      stat.setDailyDaoDeposit(dailyDaoDeposit);

      // 总存款数 = 当日存款 - 当日提款 + 昨日累计
      var totalDaoDeposit = calculateTotalDaoDeposit(startedAt, endedAt, yesterdayStat, dailyDaoDeposit);
      stat.setTotalDaoDeposit(totalDaoDeposit.toString());

      // 到当天结束时间的存款人数
      stat.setDaoDepositorsCount(calculateDaoDepositorsCount(endedAt).toString());

      // 到当天结束时间的未认领补偿
      var phase1DaoInterestsWithDepositTime = calculatePhase1DaoInterestsWithDepositTime(endedAt);
      var unmadeDaoInterestsWithDepositTime = calculateUnmadeDaoInterests(endedAt,commonStatisticInfo.getMaxBlockNumber());
      
      // 添加null检查以避免NPE
      BigInteger phase1Interests = (phase1DaoInterestsWithDepositTime != null) ? phase1DaoInterestsWithDepositTime.getTotalPhase1DaoInterests() : BigInteger.ZERO;
      BigInteger unmadeInterests = (unmadeDaoInterestsWithDepositTime != null) ? unmadeDaoInterestsWithDepositTime.getTotalUnmadeDaoInterests() : BigInteger.ZERO;
      BigInteger unclaimedCompensation = phase1Interests.add(unmadeInterests);
      
      // 确保结果非负
      if (unclaimedCompensation.compareTo(BigInteger.ZERO) < 0) {
        log.warn("日期 {} 的未认领补偿为负，调整为0: {}", date, unclaimedCompensation);
        unclaimedCompensation = BigInteger.ZERO;
      }
      stat.setUnclaimedCompensation(unclaimedCompensation.toString());

      // 到当天结束时间的已认领的补偿 = todayClaimed + yesterdayClaimed
      BigInteger claimedCompensation = calculateClaimedCompensation(startedAt, endedAt, yesterdayStat);
      stat.setClaimedCompensation(claimedCompensation.toString());

      // 到当天结束时间的存款补偿 = 未认领的补偿 + 已认领的补偿
      BigInteger depositCompensation = calculateDepositCompensation(unclaimedCompensation, claimedCompensation);
      // 确保结果非负
      if (depositCompensation.compareTo(BigInteger.ZERO) < 0) {
        log.warn("日期 {} 的存款补偿为负，调整为0: {}", date, depositCompensation);
        depositCompensation = BigInteger.ZERO;
      }
      stat.setDepositCompensation(depositCompensation.toString());

      // 平均存款的时长
      var averageDepositTime = calculateAverageDepositTime(phase1DaoInterestsWithDepositTime, unmadeDaoInterestsWithDepositTime);
      // 确保结果非负
      if (averageDepositTime != null && averageDepositTime.compareTo(BigDecimal.ZERO) < 0) {
        log.warn("日期 {} 的平均存款时间为负，调整为0: {}", date, averageDepositTime);
        averageDepositTime = BigDecimal.ZERO;
      }
      stat.setAverageDepositTime(averageDepositTime != null ? averageDepositTime.toString() : "0");

//    stat.setEstimatedApc(calculateEstimatedApc(startedAt, endedAt, yesterdayStat)); 页面不查，先不管

      // 国库金额 CKB总供应量
      var treasuryAmount = calculateTreasuryAmount(commonStatisticInfo.getMaxBlockNumber(), unclaimedCompensation);
      stat.setTreasuryAmount(treasuryAmount.toString());

      // 总存款者数量(包含了已经提款的)
      stat.setTotalDepositorsCount(calculateTotalDepositorsCount(endedAt).toString());

      // 当天存款人数(新地址）
      Integer dailyDaoDepositorsCount = calculateDailyDaoDepositorsCount(startedAt, endedAt);
      stat.setDailyDaoDepositorsCount(dailyDaoDepositorsCount);

//    stat.setDailyDaoWithdraw(calculateDailyDaoWithdraw(startedAt, endedAt));页面不查，先不管

      // 获取行情数据
      MarketDataDto marketData = marketDataService.getMarketData(commonStatisticInfo.getMaxBlockNumber(), "shannon");

      BigDecimal circulatingSupply = marketData.getCirculatingSupply();
      stat.setCirculatingSupply(circulatingSupply);

      stat.setCirculationRatio(calculateCirculationRatio(new BigDecimal(totalDaoDeposit), circulatingSupply));

      //stat.setTotalSupply(calculateTotalSupply(stat.getTotalDaoDeposit(), circulatingSupply)); 页面不查，先不管
      stat.setLockedCapacity(calculateLockedCapacity(marketData));

      if (marketData.getParsedDao() != null) {
        stat.setKnowledgeSize(calculateKnowledgeSize(marketData.getParsedDao()));
      } else {
        stat.setKnowledgeSize(BigInteger.ZERO);
      }

      // 截止到endedAt的占用空间
      var occupiedCapacityWithHolderCount = calculateOccupiedCapacityWithHolderCount(startedAt, endedAt, date, yesterdayStat);
      stat.setOccupiedCapacity(occupiedCapacityWithHolderCount != null ? occupiedCapacityWithHolderCount.getOccupiedCapacity(): BigInteger.ZERO);

      // SECONDARY_EPOCH_REWARD * (epoch_number + (index/length)) - treasuryAmount - DepositCompensation
      stat.setMiningReward(calculateMiningReward(commonStatisticInfo.getMaxBlockNumber(), depositCompensation, treasuryAmount).toString());
      stat.setCkbHodlWave(calculateCkbHodlWave(startedAt, endedAt));
      Long holderCount = occupiedCapacityWithHolderCount != null && occupiedCapacityWithHolderCount.getHolderCount() != null ?occupiedCapacityWithHolderCount.getHolderCount() : 0;
      stat.setHolderCount(holderCount);
      stat.setActivityAddressContractDistribution(
          calculateActivityAddressContractDistribution(commonStatisticInfo.getMaxBlockNumber(), commonStatisticInfo.getMinBlockNumber()));
      stat.setBlockTimeDistribution(new ListStringWrapper(calculateBlockTimeDistribution(commonStatisticInfo.getMaxBlockNumber())));
      stat.setEpochTimeDistribution(new ListStringWrapper(calculateEpochTimeDistribution()));
      
      baseMapper.insertOrUpdate(stat);
      log.info("日期 {} 的统计数据生成完成", date);
      return true;
    } catch (Exception e) {
      log.error("生成日期 {} 的统计数据时出错", date, e);
      // 在实际环境中，可能需要记录错误状态以便后续重试
      return false;
    }
  }

  private CommonStatisticInfoDto calculateCommonStatisticInfo(Long startedAt, Long endedAt) {
    return blockService.getCommonStatisticInfo(startedAt, endedAt);
  }

  /**
   * 获取前一天统计数据
   */
  private DailyStatistics getYesterdayStatistic(Long date) {
    return baseMapper.findDailyStatistic(date - SECONDS_IN_DAY);
  }

    // 2. 地址数量统计（对应addresses_count逻辑）
    public Long calculateAddressesCount(Long startedAt, Long endedAt, DailyStatistics yesterdayStat) {
        // 昨日统计不存在
        if (yesterdayStat == null) {
            // 从创世到结束时间的所有地址
            return scriptMapper.getAddressesCount(endedAt);
        } else {
            // 当天新增地址 + 昨日累计地址
            long todayNew = scriptMapper.getAddressesCountByTimeRange(startedAt, endedAt);
            return todayNew + yesterdayStat.getAddressesCount();
        }
    }

     // 3. 总DAO存款（对应total_dao_deposit逻辑）
    public BigInteger calculateTotalDaoDeposit(Long startedAt, Long endedAt, DailyStatistics yesterdayStat,  BigInteger dailyDaoDeposit) {

      if (yesterdayStat == null || yesterdayStat.getTotalDaoDeposit() == null) {
        // 创世到结束时间的总存款 - 总提款
        BigInteger totalDeposit = depositCellMapper.totalDeposit(endedAt);
        totalDeposit = totalDeposit == null ? BigInteger.ZERO : totalDeposit;
        BigInteger totalWithdraw = withdrawCellMapper.totalWithdraw(endedAt);
        totalWithdraw = totalWithdraw == null ? BigInteger.ZERO : totalWithdraw;
        return totalDeposit.subtract(totalWithdraw);
      } else {
            // 当日存款 - 当日提款 + 昨日累计
        BigInteger todayWithdraw = withdrawCellMapper.getDailyWithdraw(startedAt, endedAt);
        todayWithdraw = todayWithdraw == null ? BigInteger.ZERO : todayWithdraw;
        BigInteger yesterdayTotal = new BigInteger(yesterdayStat.getTotalDaoDeposit());
        return dailyDaoDeposit.subtract(todayWithdraw).add(yesterdayTotal);
      }
    }

    // 6. 活细胞数量（对应live_cells_count逻辑）
    public Long calculateLiveCellsCount(Long liveCellCount, DailyStatistics yesterdayStat) {
        if (yesterdayStat == null) {
            // 创世到结束时间未消耗的细胞
            return liveCellCount == null ? 0L : liveCellCount;
        } else {
            liveCellCount = liveCellCount == null ? 0L : liveCellCount;
            // 当日变更细胞 + 昨日活细胞
            return Long.parseLong(yesterdayStat.getLiveCellsCount()) + liveCellCount;
        }
    }

    // 7. 死亡细胞数量计算
    public Long calculateDeadCellsCount(Long startedAt, Long endedAt, DailyStatistics yesterdayStat) {
      if (yesterdayStat == null) {
          var deadCellsCount = ckbTransactionMapper.getDeadCellsCountByTime(startedAt, endedAt);
          return deadCellsCount == null ? 0L : deadCellsCount;
        } else {
          Long todayDead = ckbTransactionMapper.getDeadCellsCountByTime(startedAt, endedAt);
          todayDead = todayDead == null ? 0L : todayDead;
          return todayDead + Long.parseLong(yesterdayStat.getDeadCellsCount());
        }
    }

    // 8. 平均哈希率计算
    public BigDecimal calculateAvgHashRate(BigDecimal totalDifficulty, Long totalBlockTime) {
        return totalBlockTime > 0 ? totalDifficulty.divide(new BigDecimal(totalBlockTime), 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    // 9. 平均难度计算
    public BigDecimal calculateAvgDifficulty(BigDecimal totalDifficulty, Long blockCount) {
        return blockCount > 0 ? totalDifficulty.divide(new BigDecimal(blockCount), 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    // 10. 叔块率计算
    public BigDecimal calculateUncleRate(Long totalUncles, Long totalBlocks) {
        return totalBlocks > 0 ? new BigDecimal(totalUncles).divide(new BigDecimal(totalBlocks), 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    // 11. 总存款人数计算
    public Long calculateTotalDepositorsCount(Long endedAt) {
        return depositCellMapper.getTotalDepositorsCount(endedAt);
    }

    // 12. 地址余额分布计算
    public List<String[]> calculateAddressBalanceDistribution() {

      var zeroLockScriptIds = scriptMapper.getZeroLockScriptId(Numeric.hexStringToByteArray(ZERO_LOCK_CODE_HASH));
      var list = statisticAddressMapper.getAddressBalanceDistribution(zeroLockScriptIds);

      return list.stream()
          .map(item -> new String[]{item.getRangeUpper().toString(), item.getAddressCount().toString(), item.getCumulativeCount().toString()})
          .collect(Collectors.toList());
    }

    // 14. 占用容量计算 CellOutput.generated_before(ended_at).unconsumed_at(ended_at).sum(:occupied_capacity)
    public OccupiedCapacityWithHolderCountDto calculateOccupiedCapacityWithHolderCount(Long startedAt, Long endedAt, Long date, DailyStatistics yesterdayStat) {

      log.info("calculateOccupiedCapacityWithHolderCount date: {}",  date);
      // 从缓存获取前一天的统计数据
      Long yesterday = yesterdayStat == null ? date - SECONDS_IN_DAY :yesterdayStat.getCreatedAtUnixtimestamp();
      Map<Long, BigInteger> history = getLastLockScriptIdWithOccupiedCapacity(yesterday);
      // 如果缓存不存在,则查询到今天为止的占用容量和持有人
      if(history == null || history.isEmpty()){
        Map<Long, BigInteger> map = calculateHistoryOccupiedCapacityWithHolderCount(endedAt);
        // 写入缓存
        setLastLockScriptIdWithOccupiedCapacity(map, date);
        return new OccupiedCapacityWithHolderCountDto(map.values().stream().reduce(BigInteger::add).orElse(BigInteger.ZERO), Long.valueOf(map.size()));
      }

      Long holderCount = yesterdayStat==null? history.size(): yesterdayStat.getHolderCount();
      BigInteger totalOccupiedCapacity = yesterdayStat==null? history.values().stream().reduce(BigInteger::add).orElse(BigInteger.ZERO): yesterdayStat.getOccupiedCapacity();
      // 查询当日新增的cell相关的lockScriptId 跟 occupiedCapacity
      List<LockScriptIdWithOccupiedCapacityDto> added = outputMapper.getLockScriptIdWithAddedOccupiedCapacityByDate(startedAt, endedAt);
      // 处理新增的cell
      for (LockScriptIdWithOccupiedCapacityDto addCell : added) {
        Long lockScriptId = addCell.getLockScriptId();
        BigInteger occupiedCapacity = addCell.getOccupiedCapacity();
        if (history.containsKey(lockScriptId)) {
          history.put(lockScriptId, history.get(lockScriptId).add(occupiedCapacity));
        } else {
          history.put(lockScriptId, occupiedCapacity);
          holderCount ++;
        }
        totalOccupiedCapacity = totalOccupiedCapacity.add(occupiedCapacity);
       }

      // 查询当日getLockScriptIdWithConsumedOccupiedCapacityByDate
      List<LockScriptIdWithOccupiedCapacityDto> consumed = outputMapper.getLockScriptIdWithConsumedOccupiedCapacityByDate(startedAt, endedAt);
      // 处理消耗的cell
      for (LockScriptIdWithOccupiedCapacityDto consumedCell : consumed) {
        Long lockScriptId = consumedCell.getLockScriptId();
        BigInteger consumedCapacity = consumedCell.getOccupiedCapacity();
        if (history.containsKey(lockScriptId)) {
          BigInteger current = history.get(lockScriptId);
          BigInteger newCapacity = current.subtract(consumedCapacity);
          if(newCapacity.compareTo(BigInteger.ZERO) <= 0){
            holderCount --;
            history.remove(lockScriptId);
          } else{
            history.put(lockScriptId, newCapacity);
          }
          totalOccupiedCapacity = totalOccupiedCapacity.subtract(consumedCapacity);
        }
      }

      // 写入缓存
      setLastLockScriptIdWithOccupiedCapacity(history, date);

      return new OccupiedCapacityWithHolderCountDto(totalOccupiedCapacity, holderCount);
    }

    private Map<Long, BigInteger> calculateHistoryOccupiedCapacityWithHolderCount(Long endedAt){
      List<LockScriptIdWithOccupiedCapacityDto> list = outputMapper.getHistoryOccupiedCapacityWithHolderCount(endedAt);
      return list.stream().collect(Collectors.toMap(LockScriptIdWithOccupiedCapacityDto::getLockScriptId, LockScriptIdWithOccupiedCapacityDto::getOccupiedCapacity));
    }

    // 15. 每日DAO存款计算
    public BigInteger calculateDailyDaoDeposit(Long startedAt, Long endedAt) {
      var dailyDaoDeposit = depositCellMapper.getDailyDaoDeposit(startedAt, endedAt);
      return dailyDaoDeposit == null ? BigInteger.ZERO : dailyDaoDeposit;
    }

    // 16. 每日新增DAO存款人数计算
    public Integer calculateDailyDaoDepositorsCount(Long startedAt, Long endedAt) {
        Set<Long> existingDepositors = depositCellMapper.existingDepositors(startedAt);

        Set<Long> todayDepositors = depositCellMapper.todayDepositors(startedAt, endedAt);

        todayDepositors.removeAll(existingDepositors);
        return todayDepositors.size();
    }

    // 17. 流通率计算
    public BigDecimal calculateCirculationRatio(BigDecimal totalDaoDeposit, BigDecimal circulatingSupply) {
        if (circulatingSupply.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalDaoDeposit.divide(circulatingSupply, 4, RoundingMode.HALF_UP);
    }

    // 18. 知识量计算 u_i - (BURN_QUOTA * 0.6)
    public BigInteger calculateKnowledgeSize(DaoDto parsedDao) {
        return parsedDao.getUI().subtract(BURN_QUOTA.multiply(new BigInteger("6")).divide(new BigInteger("10")));
    }

  private List<String[]> calculateBlockTimeDistribution(Long maxBlockNumber) {
    var startBlockNumber = maxBlockNumber > 50001 ? maxBlockNumber - 50000 : 2;// 忽略前两块的出块时间
    var result = blockService.getBlockTimeDistribution(startBlockNumber, maxBlockNumber);
    result = result == null? new ArrayList<>():result;
    // 将result转成[["0.1", "6"],["0.2", "5"]……["50.0", "0"]]的格式
    return result.stream().map(item -> new String[]{String.valueOf(item.getSecondUpperBound()),  String.valueOf(item.getBlockCount())})
        .collect(Collectors.toList());

  }

  private List<String[]> calculateEpochTimeDistribution() {
    var list = epochStatisticMapper.getEpochTimeDistribution();

    return list.stream()
        .map(item -> new String[]{item.getRangeUpper().toString(), item.getEpochCount().toString()})
        .collect(Collectors.toList());

  }
    // 19. CKB持有周期分布计算
    public Map<String, String> calculateCkbHodlWave(Long startAt, Long endAt) {

      Instant endInstant = Instant.ofEpochMilli(endAt);

      // 转为 UTC 时区的 ZonedDateTime
      ZonedDateTime endZdt = endInstant.atZone(ZoneOffset.UTC);

      CkbHodlWaveDto dto = outputMapper.getCkbHodlWave(startAt, endAt,
          endZdt.minusYears(3).toInstant().toEpochMilli(),
          endZdt.minusYears(1).toInstant().toEpochMilli(),
          endZdt.minusMonths(6).toInstant().toEpochMilli(),
          endZdt.minusMonths(3).toInstant().toEpochMilli(),
          endZdt.minusMonths(1).toInstant().toEpochMilli(),
          endZdt.minusDays(7).toInstant().toEpochMilli());

      // 提取字段为 BigDecimal
      Map<String, BigDecimal> ckbValues = new LinkedHashMap<>();
      ckbValues.put("overThreeYears", toCkb(dto.getOverThreeYears()));
      ckbValues.put("oneYearToThreeYears", toCkb(dto.getOneYearToThreeYears()));
      ckbValues.put("sixMonthsToOneYear", toCkb(dto.getSixMonthsToOneYear()));
      ckbValues.put("threeMonthsToSixMonths", toCkb(dto.getThreeMonthsToSixMonths()));
      ckbValues.put("oneMonthToThreeMonths", toCkb(dto.getOneMonthToThreeMonths()));
      ckbValues.put("oneWeekToOneMonth", toCkb(dto.getOneWeekToOneMonth()));
      ckbValues.put("dayToOneWeek", toCkb(dto.getDayToOneWeek()));
      ckbValues.put("latestDay", toCkb(dto.getLatestDay()));

      // 计算 total
      BigDecimal total = ckbValues.values().stream()
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      // 构建结果：每个字段是 CKB 数量（字符串）
      Map<String, String> result = new LinkedHashMap<>();
      for (Map.Entry<String, BigDecimal> entry : ckbValues.entrySet()) {
        result.put(entry.getKey(), formatDecimal(entry.getValue()));
      }

      // 添加 total_supply
      result.put("totalSupply", formatDecimal(total));

      return result;
    }

  private BigDecimal toCkb(BigInteger shannon) {
    return new BigDecimal(shannon != null ? shannon : BigInteger.ZERO)
        .divide(SHANNON_TO_CKB, 8, RoundingMode.DOWN); // 截断，不四舍五入
  }

  private String formatDecimal(BigDecimal value) {
    // 检查数值是否为整数（小数部分为0）
    if (value.scale() <= 0 || value.stripTrailingZeros().scale() <= 0) {
      // 整数：强制保留1位小数（.0）
      return value.setScale(1, RoundingMode.UNNECESSARY).toPlainString();
    } else {
      // 非整数：去除尾随零后直接转换（保留原始有效小数位）
      return value.stripTrailingZeros().toPlainString();
    }
  }

    // 21. 活动地址合约分布计算
    public Map<String, String> calculateActivityAddressContractDistribution(Long maxBlockNumber,
        Long minBlockNumber) {

      List<ActivityAddressContractDistributionDto> codeHashCount = outputMapper.getActivityAddressContractDistribution(
          maxBlockNumber, minBlockNumber);

      // 转换为合约名称映射
      Map<String, String> contractCount = new LinkedHashMap<>();
      long others = 0;

      for (ActivityAddressContractDistributionDto dto : codeHashCount) {
        // 跳过无效数据（防御性编程）
        if (dto == null || dto.getCodeHash() == null) {
          continue;
        }
        String codeHash = dto.getCodeHash();
        Long count = dto.getScriptHashCount() == null ? 0 : dto.getScriptHashCount();
        // 获取对应合约名
        var lockScript = scriptConfig.getLockScriptByCodeHash(codeHash);

        if (lockScript != null) {
          contractCount.put(lockScript.getName(), String.valueOf(count) );
        } else {
          others += dto.getScriptHashCount();
        }
      }

      if (others > 0) {
        contractCount.put("Others", String.valueOf(others));
      }

      return contractCount;
    }

    // 22. 挖矿奖励计算 SECONDARY_EPOCH_REWARD * (epoch_number + (index/length)) - treasuryAmount - DepositCompensation
    public BigDecimal calculateMiningReward(Long maxBlockNumber, BigInteger depositCompensation, BigInteger treasuryAmount) {
        EpochDto epochInfo = null;

        // 特殊处理
        if(maxBlockNumber == 0L){
          epochInfo = blockService.getEpochByBlockNumber(maxBlockNumber+1);
          epochInfo.setIndex(0);
        } else{
          epochInfo = blockService.getEpochByBlockNumber(maxBlockNumber);
        }

        var total = SECONDARY_EPOCH_REWARD.multiply(BigDecimal.valueOf(epochInfo.getNumber()).add(BigDecimal.valueOf(epochInfo.getIndex()+1).divide(BigDecimal.valueOf(epochInfo.getLength()),15,RoundingMode.HALF_UP)));
        var miningReward = total.subtract(new BigDecimal(treasuryAmount)).subtract(new BigDecimal(depositCompensation));
        return miningReward.compareTo(BigDecimal.ZERO) < 0? BigDecimal.ZERO : miningReward.setScale(0, RoundingMode.DOWN);
    }

    // 23. 存款补偿计算
    public BigInteger calculateDepositCompensation(BigInteger unclaimed, BigInteger claimed) {
        return unclaimed.add(claimed);
    }

    // 24. 存款利息计算
    public BigDecimal calculateAverageDepositTime(Phase1DaoInterestsWithAverageDepositTimeDto phase1DaoInterestsWithDepositTime, UnmadeDaoInterestsWithTimeDto unmadeDaoInterestsWithDepositTime){

      BigInteger totalDeposits = phase1DaoInterestsWithDepositTime.getInterestBearingDeposits().add(unmadeDaoInterestsWithDepositTime.getUninterestBearingDeposits());
      // 总时间贡献 / 总存款，截断到3位小数（对应Ruby的truncate(3)）
      BigDecimal sumBearing = phase1DaoInterestsWithDepositTime.getSumInterestBearing().add(unmadeDaoInterestsWithDepositTime.getSumUninterestBearing());
      return totalDeposits.equals(BigInteger.ZERO)
          ? BigDecimal.ZERO : sumBearing.divide(new BigDecimal(totalDeposits), 3, RoundingMode.DOWN);
    }

    // 25. 已认领补偿计算 = todayClaimed + yesterdayClaimed
    public BigInteger calculateClaimedCompensation(Long startedAt, Long endedAt, DailyStatistics yesterdayStat) {

      List<DaoCellDto> todayCells;
      BigInteger yesterdayClaimed;
      if (yesterdayStat == null || yesterdayStat.getClaimedCompensation() == null) {
        todayCells = withdrawCellMapper.getConsumedCellsByEndTime(endedAt);
        yesterdayClaimed = BigInteger.ZERO;
      } else {
        todayCells = withdrawCellMapper.getConsumedCellsByStartTimeEndTime(startedAt, endedAt);
        yesterdayClaimed = new BigInteger(yesterdayStat.getClaimedCompensation());
      }
      BigInteger todayClaimed = calculateDaoInterestsSum(todayCells);
      return todayClaimed.add(yesterdayClaimed);
    }

    // 辅助方法：计算DAO利息总和
    private BigInteger calculateDaoInterestsSum(List<DaoCellDto> cells) {
      BigInteger total = BigInteger.ZERO;
      Set<Long> depositBlockNumbers = cells.stream()
          .map(withdrawCell -> CkbUtil.convertToBlockNumber(withdrawCell.getData())).collect(Collectors.toSet());
      Set<Long> withdrawBlockNumbers = cells.stream().map(DaoCellDto::getBlockNumber)
          .collect(Collectors.toSet());
      depositBlockNumbers.addAll(withdrawBlockNumbers);
      // 获取所有相关区块的DAO信息
      Map<Long, byte[]> blockDaos = blockService.getBlockDaos(depositBlockNumbers);

      for (DaoCellDto withdrawCell : cells) {
        total = total.add(
            DaoCompensationCalculator.call(withdrawCell,
                blockDaos.get(withdrawCell.getBlockNumber()),
                blockDaos.get(CkbUtil.convertToBlockNumber(withdrawCell.getData()))));
      }
      return total;
    }

  // 辅助方法：计算DAO利息总和(带存款时间与金额)
  private DaoInterestsSumWithTimeDto calculateDaoInterestsSumWithTime(List<DaoCellDto> cells) {
    BigInteger total = BigInteger.ZERO;
    BigDecimal sumInterestBearing = BigDecimal.ZERO;
    BigInteger interestBearingDeposits = BigInteger.ZERO;
    DaoInterestsSumWithTimeDto result = new DaoInterestsSumWithTimeDto();
    Set<Long> depositBlockNumbers = cells.stream()
        .map(withdrawCell -> CkbUtil.convertToBlockNumber(withdrawCell.getData())).collect(Collectors.toSet());
    Set<Long> withdrawBlockNumbers = cells.stream().map(DaoCellDto::getBlockNumber)
        .collect(Collectors.toSet());
    depositBlockNumbers.addAll(withdrawBlockNumbers);
    // 获取所有相关区块的DAO信息
    List<BlockDaoDto> blockWithBlockTimestamp = blockService.getBlockDaoWithBlockTimestamp(depositBlockNumbers);

    Map<Long, byte[]> blockDaos = blockWithBlockTimestamp.stream()
        .collect(Collectors.toMap(BlockDaoDto::getBlockNumber, BlockDaoDto::getDao));
    Map<Long, Long> blockTimestamps = blockWithBlockTimestamp.stream()
        .collect(Collectors.toMap(BlockDaoDto::getBlockNumber, BlockDaoDto::getTimestamp));
    for (DaoCellDto withdrawCell : cells) {
      total = total.add(
          DaoCompensationCalculator.call(withdrawCell,
              blockDaos.get(withdrawCell.getBlockNumber()),
              blockDaos.get(CkbUtil.convertToBlockNumber(withdrawCell.getData()))));
      long timeDiffMs = withdrawCell.getBlockTimestamp() - blockTimestamps.get(CkbUtil.convertToBlockNumber(withdrawCell.getData()));
      var capacity = withdrawCell.getValue(); // 存款金额
      if (timeDiffMs > 0) {
        var days = BigDecimal.valueOf(timeDiffMs).divide( // 存款时间总和 单位天
            MILLISECONDS_IN_DAY,
            6, // 保留6位小数，避免中间计算误差
            RoundingMode.HALF_UP
        );
        sumInterestBearing  = sumInterestBearing.add(new BigDecimal(capacity).multiply(days)); // 时间贡献 = 存款金额 * 存款时间
      }
      interestBearingDeposits = interestBearingDeposits.add(capacity);
    }
    result.setDaoInterestsSum( total); // 存款利息总和
    result.setInterestBearingDeposits(interestBearingDeposits); // 存款金额总和
    result.setSumInterestBearing(sumInterestBearing);// 存款时间贡献总和
    return result;
  }


    // 辅助方法：计算当天总难度
    private BigDecimal calculateTotalDifficultiesForDay(Long startedAt, Long endedAt) {
        List<EpochWithBlockNumberDto> epochNumbers = blockService.findEpochWithBlockNumberByTime(startedAt, endedAt);
        BigDecimal total = BigDecimal.ZERO;
        for (EpochWithBlockNumberDto epoch : epochNumbers) {
          var difficulty = TypeConversionUtil.byteConvertToUInt256(epoch.getDifficulty());
            total = total.add(new BigDecimal(difficulty).multiply(new BigDecimal(epoch.getLastBlockNumber()-epoch.getFirstBlockNumber()+1)));
        }

        return total;
    }


    /**
     * 获取到endedAt为止phase1的DAO补偿
     * @param endedAt 截止时间
     */
    private Phase1DaoInterestsWithAverageDepositTimeDto calculatePhase1DaoInterestsWithDepositTime(Long endedAt) {
      log.debug("开始计算phase1的DAO补偿，截止时间: {}", endedAt);
      BigInteger totalPhase1DaoInterests = BigInteger.ZERO;
      BigInteger interestBearingDeposits = BigInteger.ZERO;
      BigDecimal sumInterestBearing = BigDecimal.ZERO;
      var result = new Phase1DaoInterestsWithAverageDepositTimeDto();
      // 分页查询：避免一次性加载大量数据（模拟Ruby的find_each）
      int page = 1;
      int totalProcessed = 0;
      while (true) {
        Page<DaoCellDto> pageable = new Page<>(page, daoPageSize);
        Page<DaoCellDto> nervosDaoWithdrawingCellsPage = withdrawCellMapper.getUnConsumedCellsByEndTime(pageable, endedAt);
        if (nervosDaoWithdrawingCellsPage.getRecords().isEmpty()) {
          break; // 无更多数据，退出循环
        }
        var nervosDaoWithdrawingCells = nervosDaoWithdrawingCellsPage.getRecords();
        var phase1DaoInterestsWithTime = calculateDaoInterestsSumWithTime(nervosDaoWithdrawingCells);
        
        // 添加检查避免计算错误
        if (phase1DaoInterestsWithTime != null) {
          totalPhase1DaoInterests = totalPhase1DaoInterests.add(phase1DaoInterestsWithTime.getDaoInterestsSum());
          interestBearingDeposits = interestBearingDeposits.add(phase1DaoInterestsWithTime.getInterestBearingDeposits());
          sumInterestBearing = sumInterestBearing.add(phase1DaoInterestsWithTime.getSumInterestBearing());
          totalProcessed += nervosDaoWithdrawingCells.size();
          log.debug("处理第{}页，累计处理{}条记录，当前总利息: {}", page, totalProcessed, totalPhase1DaoInterests);
        }
        page++;
      }
      result.setTotalPhase1DaoInterests(totalPhase1DaoInterests);
      result.setInterestBearingDeposits(interestBearingDeposits);
      result.setSumInterestBearing(sumInterestBearing);
      log.debug("phase1的DAO补偿计算完成，总利息: {}", totalPhase1DaoInterests);
      return result;
    }

    /**
     * 计算到endedAt为止未认领的DAO补偿
     * @param endedAt 截止时间
     * @param maxBlockNumber 最大块高
     */
    private UnmadeDaoInterestsWithTimeDto calculateUnmadeDaoInterests(Long endedAt, Long maxBlockNumber) {
      log.debug("开始计算未认领的DAO补偿，截止时间: {}, 最大块高: {}", endedAt, maxBlockNumber);
      BigInteger total = BigInteger.ZERO;
      BigInteger uninterestBearingDeposits = BigInteger.ZERO; // 无息存款总额
      BigDecimal sumUninterestBearing = BigDecimal.ZERO; // 无息存款时间贡献总和
      var result = new UnmadeDaoInterestsWithTimeDto();
      byte[] tipBlockDao = blockService.getDaoByBlockNumber(maxBlockNumber);
      if(tipBlockDao == null){
        log.warn("无法获取块高{}的DAO数据", maxBlockNumber);
        return result;
      }

      // 分页查询：避免一次性加载大量数据（模拟Ruby的find_each）
      int page = 1;
      int totalProcessed = 0;
      while (true) {
        Page<DaoCellDto> pageable = new Page<>(page, daoPageSize);
        Page<DaoCellDto> nervosDaoDepositCellsPage = depositCellMapper.getUnConsumedCellsByEndTime(pageable,
            endedAt);

        if (nervosDaoDepositCellsPage.getRecords().isEmpty()) {
          break;
        }

        var nervosDaoDepositCells = nervosDaoDepositCellsPage.getRecords();
        Set<Long> depositBlockNumbers = nervosDaoDepositCells.stream().map(DaoCellDto::getBlockNumber)
            .collect(Collectors.toSet());
        Map<Long, byte[]> blockDaos = blockService.getBlockDaos(depositBlockNumbers);

        // 使用CkbUtils计算每个单元格的DAO利息
        for (DaoCellDto cell : nervosDaoDepositCells) {
          try {
            // 添加空值检查
            if (cell != null && cell.getValue() != null && blockDaos.get(cell.getBlockNumber()) != null) {
              BigInteger cellInterest = DaoCompensationCalculator.call(cell, tipBlockDao,
                  blockDaos.get(cell.getBlockNumber()));
              // 确保计算结果非负
              if (cellInterest != null && cellInterest.compareTo(BigInteger.ZERO) >= 0) {
                total = total.add(cellInterest);
              }
              var timeDiffMs = endedAt - cell.getBlockTimestamp();
              var capacity = cell.getValue(); // 存款金额
              // 避免负数时间差
              if (timeDiffMs > 0) {
                var days = BigDecimal.valueOf(timeDiffMs).divide( // 存款时间总和 单位天
                    MILLISECONDS_IN_DAY,
                    6, // 保留6位小数，避免中间计算误差
                    RoundingMode.HALF_UP
                );
                sumUninterestBearing = sumUninterestBearing.add(new BigDecimal(capacity).multiply(days));
              }
              uninterestBearingDeposits = uninterestBearingDeposits.add(capacity);
            }
          } catch (Exception e) {
            log.error("计算单元格ID {} 的DAO利息时出错", cell.getBlockNumber(), e);
            throw e;
          }
        }
        totalProcessed += nervosDaoDepositCells.size();
        log.debug("处理第{}页，累计处理{}条记录，当前未认领利息: {}", page, totalProcessed, total);
        page++;
      }

      result.setTotalUnmadeDaoInterests(total);
      result.setUninterestBearingDeposits(uninterestBearingDeposits);
      result.setSumUninterestBearing(sumUninterestBearing);
      log.debug("未认领的DAO补偿计算完成，总利息: {}", total);
      return result;
    }

    private BigInteger calculateLockedCapacity(MarketDataDto marketData) {
        return marketData.getEcosystemLocked()
                .add(marketData.getTeamLocked())
                .add(marketData.getPrivateSaleLocked())
                .add(marketData.getFoundingPartnersLocked())
                .add(marketData.getFoundationReserveLocked())
                .add(marketData.getBugBountyLocked());
    }

  /**
   * 截止到endedAt的存款人数
   * @param endedAt 截止时间
   * @return 存款人数
   */
  private Long calculateDaoDepositorsCount(Long endedAt){
    return depositCellMapper.daoDepositorsCount(endedAt);
  }


  /**
     * 截止到endedAt的CKB总供应量
     * @param maxBlockNumber 最大区块高度
     * @param unclaimedCompensation 未领取的DAO利息
     * @return 截止到endedAt的CKB总供应量
     */
    private BigInteger calculateTreasuryAmount(Long maxBlockNumber, BigInteger unclaimedCompensation){
      log.debug("开始计算国库金额，块高: {}, 未认领利息: {}", maxBlockNumber, unclaimedCompensation);
      try {
        byte[] tipBlockDao = blockService.getDaoByBlockNumber(maxBlockNumber);
        if(tipBlockDao == null){
          log.warn("无法获取块高{}的DAO数据", maxBlockNumber);
          return BigInteger.ZERO;
        }
        
        var parseDao = CkbUtil.parseDao(tipBlockDao);
        if (parseDao == null) {
          log.warn("解析块高{}的DAO数据失败", maxBlockNumber);
          return BigInteger.ZERO;
        }

        // 确保unclaimedCompensation不为null
        if (unclaimedCompensation == null) {
          unclaimedCompensation = BigInteger.ZERO;
        }

        // 计算结果并确保非负
        BigInteger result = parseDao.getSI().subtract(unclaimedCompensation);
        if (result.compareTo(BigInteger.ZERO) < 0) {
          log.warn("国库金额计算结果为负，设置为0，SI: {}, unclaimedCompensation: {}", parseDao.getSI(), unclaimedCompensation);
          result = BigInteger.ZERO;
        }
        
        log.debug("国库金额计算完成: {}", result);
        return result;
      } catch (Exception e) {
        log.error("计算国库金额时出错，块高: {}", maxBlockNumber, e);
        return BigInteger.ZERO;
      }
    }

  @Async("asyncStatisticTaskExecutor")
  @Override
  public void reSetMiningReward(){
    // 获取分布式锁，确保只有一个实例执行reset统计
    RLock lock = redissonClient.getLock(DAILY_STATISTIC_RESET_LOCK_KEY);
    try (AutoReleaseRLock autoReleaseRLock = new AutoReleaseRLock(lock, LOCK_WAIT_TIME, TimeUnit.SECONDS)){
      log.info("开始重置每日统计表 mining_reward 字段");
        LambdaQueryWrapper<DailyStatistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(DailyStatistics::getCreatedAtUnixtimestamp);
        var dailyStatistics = baseMapper.selectList(queryWrapper);
        for(DailyStatistics item :dailyStatistics){
          var miningReward = calculateMiningReward(item.getMaxBlockNumber(), new BigInteger(item.getDepositCompensation()), new BigInteger(item.getTreasuryAmount()));
          item.setMiningReward(miningReward.toString());
          item.setUpdatedAt(OffsetDateTime.now());
        }
        baseMapper.updateById(dailyStatistics);
      log.info("重置每日统计表 mining_reward 字段完成");
    } catch (Exception e) {
      log.error("每日统计 reSetMiningReward 任务执行异常", e);
    }
  }

  @Async("asyncStatisticTaskExecutor")
  @Override
  public void reSetOccupiedCapacityWithHolderCount(){

    // 获取分布式锁，确保只有一个实例执行reset统计
    RLock lock = redissonClient.getLock(DAILY_STATISTIC_RESET_LOCK_KEY);
    try (AutoReleaseRLock autoReleaseRLock = new AutoReleaseRLock(lock, LOCK_WAIT_TIME, TimeUnit.SECONDS)){
      log.info("开始重置每日统计表 occupied_capacity holder_count 字段");
      LambdaQueryWrapper<DailyStatistics> queryWrapper = new LambdaQueryWrapper<>();
      queryWrapper.ge(DailyStatistics::getCreatedAtUnixtimestamp, 1647993600L);
      queryWrapper.orderByAsc(DailyStatistics::getId);
      var dailyStatistics = baseMapper.selectList(queryWrapper);
      DailyStatistics yesterdayStat = null;
      for(DailyStatistics item :dailyStatistics){
        Long startedAt = DateUtils.getStartedAt(item.getCreatedAtUnixtimestamp());
        Long endedAt = DateUtils.getEndedAt(item.getCreatedAtUnixtimestamp());

        var occupiedCapacityWithHolderCount = calculateOccupiedCapacityWithHolderCount(startedAt, endedAt, item.getCreatedAtUnixtimestamp(), yesterdayStat);
        item.setOccupiedCapacity(occupiedCapacityWithHolderCount != null ? occupiedCapacityWithHolderCount.getOccupiedCapacity(): BigInteger.ZERO);
        Long holderCount = occupiedCapacityWithHolderCount != null && occupiedCapacityWithHolderCount.getHolderCount() != null ?occupiedCapacityWithHolderCount.getHolderCount() : 0;
        item.setHolderCount(holderCount);
        item.setUpdatedAt(OffsetDateTime.now());
        yesterdayStat = item;
      }
      baseMapper.updateById(dailyStatistics);
      log.info("重置每日统计表 occupied_capacity holder_count 字段完成");
    } catch (Exception e) {
      log.error("每日统计 occupied_capacity holder_count 任务执行异常", e);
    }
  }

  @Override
  public DailyStatistics getHashRateByDate(Long  date){
      LambdaQueryWrapper<DailyStatistics> queryWrapper = new LambdaQueryWrapper<>();
      queryWrapper.select(DailyStatistics::getMaxBlockNumber, DailyStatistics::getAvgHashRate);
      queryWrapper.eq(DailyStatistics::getCreatedAtUnixtimestamp, date);
      return baseMapper.selectOne(queryWrapper);
  }

  @Async("asyncStatisticTaskExecutor")
  @Override
  public void reSetTreasuryAmount(){

    RLock lock = redissonClient.getLock(DAILY_STATISTIC_RESET_LOCK_KEY);
    try (AutoReleaseRLock autoReleaseRLock = new AutoReleaseRLock(lock, LOCK_WAIT_TIME, TimeUnit.SECONDS)){
      log.info("开始重置每日统计表 treasury_amount 字段");
      LambdaQueryWrapper<DailyStatistics> queryWrapper = new LambdaQueryWrapper<>();
      queryWrapper.orderByAsc(DailyStatistics::getCreatedAtUnixtimestamp);
      var dailyStatistics = baseMapper.selectList(queryWrapper);
      for(DailyStatistics item :dailyStatistics){
        var treasuryAmount = calculateTreasuryAmount(item.getMaxBlockNumber(), new BigInteger(item.getUnclaimedCompensation()));
        item.setTreasuryAmount(treasuryAmount.toString());
        var miningReward = calculateMiningReward(item.getMaxBlockNumber(), new BigInteger(item.getDepositCompensation()), treasuryAmount);
        item.setMiningReward(miningReward.toString());
        item.setUpdatedAt(OffsetDateTime.now());
      }
      baseMapper.updateById(dailyStatistics);
      log.info("重置每日统计表 treasury_amount 字段完成");
    } catch (Exception e) {
      log.error("每日统计 reSetTreasuryAmount 任务执行异常", e);
    }
  }
}




