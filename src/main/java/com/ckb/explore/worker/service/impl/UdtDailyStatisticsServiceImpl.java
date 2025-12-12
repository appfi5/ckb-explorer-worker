package com.ckb.explore.worker.service.impl;

import static com.ckb.explore.worker.constants.CommonConstantsKey.SECONDS_IN_DAY;
import static com.ckb.explore.worker.constants.RedisConstantsKey.LOCK_WAIT_TIME;
import static com.ckb.explore.worker.constants.RedisConstantsKey.UDT_LAST_PROCESSED_KEY;
import static com.ckb.explore.worker.constants.RedisConstantsKey.UDT_STATISTIC_DATE_LOCK_KEY;
import static com.ckb.explore.worker.constants.RedisConstantsKey.UDT_STATISTIC_LOCK_KEY;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ckb.explore.worker.domain.dto.UdtCommonStatisticsDto;
import com.ckb.explore.worker.entity.UdtDailyStatistics;
import com.ckb.explore.worker.enums.CellType;
import com.ckb.explore.worker.mapper.OutputMapper;
import com.ckb.explore.worker.mapper.TypeScriptExtendMapper;
import com.ckb.explore.worker.mapper.UdtDailyStatisticsMapper;
import com.ckb.explore.worker.service.UdtDailyStatisticsService;
import com.ckb.explore.worker.service.helper.UdtDailyStatisticsTransactionHelper;
import com.ckb.explore.worker.utils.AutoReleaseRLock;
import com.ckb.explore.worker.utils.CollectionUtils;
import com.ckb.explore.worker.utils.DateUtils;
import jakarta.annotation.Resource;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UdtDailyStatisticsServiceImpl extends
    ServiceImpl<UdtDailyStatisticsMapper, UdtDailyStatistics> implements
    UdtDailyStatisticsService {

  // 分批大小：根据 PG 性能调整，建议 500~1000 个/批
  private static final int BATCH_SIZE = 500;

  @Resource
  TypeScriptExtendMapper typeScriptExtendMapper;

  @Resource
  RedissonClient redissonClient;

  @Resource
  OutputMapper outputMapper;

  @Resource
  UdtDailyStatisticsTransactionHelper udtDailyStatisticsTransactionHelper;

  @Override
  public Long getMaxCreatAt() {
    return baseMapper.getMaxCreatAt();
  }

  @Async("asyncStatisticTaskExecutor")
  @Override
  public void perform(Long startDate, Long endDate) {
    // 获取分布式锁，确保只有一个实例执行统计
    RLock lock = redissonClient.getLock(UDT_STATISTIC_LOCK_KEY);

    try(AutoReleaseRLock autoReleaseLock = new AutoReleaseRLock(lock, LOCK_WAIT_TIME, TimeUnit.SECONDS)) {

      log.info("udt统计,开始统计日期范围: [{}, {})", startDate, endDate);

      // 按日期步进循环处理
      Long currentDate = startDate;

      int reTryCount = 0;
      while (currentDate < endDate) {
        log.info("udt统计,处理日期: {}", currentDate);

        if (reTryCount > 2) {
          log.error("udt统计,日期 {} 统计3次失败失败，结束统计", currentDate);
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
          log.error("udt统计,日期 {} 统计失败，重试", currentDate);
          reTryCount ++;
        }
      }
    } catch (Exception e) {
      log.error("udt统计,统计任务执行异常", e);
    }
  }

  @Override
  public boolean processSingleDate(Long targetDate) {
    RLock dateLock = redissonClient.getLock(UDT_STATISTIC_DATE_LOCK_KEY + targetDate);
    try(AutoReleaseRLock autoReleaseLock = new AutoReleaseRLock(dateLock, LOCK_WAIT_TIME, TimeUnit.SECONDS)) {

      return generateStatistics(targetDate);
    } catch (Exception e) {
      log.error("udt统计，处理日期 {} 发生异常", targetDate, e);
      return false;
    }
  }

  public boolean generateStatistics(Long targetDate) {
    log.info("开始处理日期 {} 的Udt统计", targetDate);
    List<Integer> cellTypes = List.of(CellType.UDT.getValue(), CellType.XUDT.getValue(), CellType.XUDT_COMPATIBLE.getValue());
    Set<Long> udtIds = typeScriptExtendMapper.getUdtIds(cellTypes);

    Long startedAt = DateUtils.getStartedAt(targetDate); // 获取开始时间 毫秒
    Long endedAt = DateUtils.getEndedAt(targetDate); // 获取结束时间 毫秒

    if (udtIds == null || udtIds.isEmpty()) {
      log.info("日期 {} 没有需要处理的Udt", targetDate);
      // 记录处理日期
      setLastProcessedDate(targetDate);
      return true;
    }

    List<UdtDailyStatistics> udtDailyStatisticsList = baseMapper.selectByDate(targetDate);
    if (udtDailyStatisticsList != null && !udtDailyStatisticsList.isEmpty()) {
      // 转成Map<Long, UdtDailyStatistics>
      Map<Long, UdtDailyStatistics> udtDailyStatisticsMap = udtDailyStatisticsList.stream().collect(
          Collectors.toMap(
              UdtDailyStatistics::getScriptId, // 键提取器：获取 udtId 作为键
              Function.identity(), // 值提取器：直接使用对象本身作为值
              (existing, replacement) -> existing // 冲突解决：若 udtId 重复，保留第一个元素
          ));
      log.info("更新日期 {} 的Udt统计数据", targetDate);
      // 重新统计

      Map<Long, UdtCommonStatisticsDto> commonStatisticsMap = batchQueryUdtStatistics(endedAt, udtIds);

      commonStatisticsMap.forEach((udtId, commonStatistics) -> {
        // 旧统计信息
        UdtDailyStatistics udtDailyStatistics = udtDailyStatisticsMap.get(udtId);

        // 创建新统计信息
        if (udtDailyStatistics == null) {
          udtDailyStatistics = new UdtDailyStatistics();
          udtDailyStatistics.setScriptId(udtId);
          udtDailyStatistics.setHoldersCount(commonStatistics.getHoldersCount());
          udtDailyStatistics.setCkbTransactionsCount(commonStatistics.getCkbTransactionsCount());
          udtDailyStatistics.setCreatedAtUnixtimestamp(targetDate);
          udtDailyStatisticsList.add(udtDailyStatistics);
          // 更新旧统计信息
        } else {
          udtDailyStatistics.setHoldersCount(commonStatistics.getHoldersCount());
          udtDailyStatistics.setCkbTransactionsCount(commonStatistics.getCkbTransactionsCount());
          udtDailyStatistics.setUpdatedAt(OffsetDateTime.now());
        }
      });
    } else {
      Long maxCreatAt = getMaxCreatAt();
      log.info("生成日期 {} 的Udt统计数据", targetDate);

      Map<Long, UdtCommonStatisticsDto> commonStatisticsMap = batchQueryUdtStatistics(endedAt, udtIds);
      // 表里无数据，且查不到统计数据，则只记录日期，不落表
      if(maxCreatAt == null &&  commonStatisticsMap.isEmpty()){
        setLastProcessedDate(targetDate);
        return true;
      }

      commonStatisticsMap.forEach((udtId, commonStatistics) -> {
        // 新统计信息
        UdtDailyStatistics udtDailyStatistics = new UdtDailyStatistics();
        udtDailyStatistics.setScriptId(udtId);
        udtDailyStatistics.setHoldersCount(commonStatistics.getHoldersCount());
        udtDailyStatistics.setCkbTransactionsCount(commonStatistics.getCkbTransactionsCount());
        udtDailyStatistics.setCreatedAtUnixtimestamp(targetDate);
        udtDailyStatisticsList.add(udtDailyStatistics);
      });
    }

    udtDailyStatisticsTransactionHelper.batchInsertOrUpdate(udtDailyStatisticsList);
    return true;
  }

  /**
   * 设置最后处理成功的日期
   */
  private void setLastProcessedDate(Long date) {
    RBucket<String> bucket = redissonClient.getBucket(UDT_LAST_PROCESSED_KEY);
    bucket.set(date.toString());
  }

  /**
   * 分批查询 UDT 统计数据
   */
  public Map<Long, UdtCommonStatisticsDto> batchQueryUdtStatistics(Long endedAt, Set<Long> udtIds) {
    Map<Long, UdtCommonStatisticsDto> resultMap = new ConcurrentHashMap<>();

    // 按批次分割 UDT ID 列表
    List<List<Long>> batches = CollectionUtils.splitIntoBatches(udtIds, BATCH_SIZE);

    batches.parallelStream().forEach(batchIds -> {
      List<UdtCommonStatisticsDto> batchResult = outputMapper.getCommonStatistics(endedAt, batchIds);
      // 存入结果 map
      batchResult.forEach(dto -> resultMap.put(dto.getScriptId(), dto));
    });

    return resultMap;
  }

}
