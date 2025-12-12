package com.ckb.explore.worker.task;

import static com.ckb.explore.worker.constants.RedisConstantsKey.LOCK_WAIT_TIME;

import com.ckb.explore.worker.domain.dto.AverageBlockTimeDto;
import com.ckb.explore.worker.entity.RollingAvgBlockTime;
import com.ckb.explore.worker.utils.AutoReleaseRLock;
import com.ckb.explore.worker.utils.JsonUtil;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;
import com.ckb.explore.worker.service.AverageBlockTimeService;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * AverageBlockTimeGenerator 平均区块时间生成器 每小时执行一次，刷新物化视图并生成缓存文件
 */
@Component
@Slf4j
public class AverageBlockTimeGenerator {

  private static final String LOCK_NAME = "averageBlockTimeGenerator";
  private static final String CACHE_KEY = "average_block_time";
  private static final long TTL_MINUTES= 75;// 多存几分钟，防止缓存失效了，定时任务还没刷新进去
  @Resource
  private RedissonClient redissonClient;

  @Resource
  private AverageBlockTimeService averageBlockTimeService;

  // 定时任务（每小时整点执行）
  @Scheduled(cron = "${cron.averageBlockTime:0 0 * * * ?}")
  public void perform() {
    log.info("定时任务触发，执行平均区块时间任务");
    try {
      executeCoreTask();
    } catch (Exception e) {
      log.error("定时任务执行失败", e);
    }
  }

  public void executeCoreTask() {
    RLock lock = redissonClient.getLock(LOCK_NAME);
    try(AutoReleaseRLock autoReleaseRLock = new AutoReleaseRLock(lock, LOCK_WAIT_TIME, TimeUnit.SECONDS))  {

      // 刷新平均区块时间相关的物化视图
      averageBlockTimeService.refreshAverageBlockTimeByHour();
      averageBlockTimeService.refreshRollingAvgBlockTime();
      // 生成缓存文件
      generateCache();
    } catch (Exception e) {
      log.error("Error in AverageBlockTimeGenerator", e);
    }
  }

  /**
   * 生成缓存文件 改成存redis
   */
  private void generateCache() {

    // 1. 查询全量滚动平均区块时间（默认按timestamp升序）
    List<RollingAvgBlockTime> avgBlockTimes = averageBlockTimeService.getAllRollingAvgBlockTime();
    AverageBlockTimeDto avgBlockTimeDto = new AverageBlockTimeDto();
    avgBlockTimeDto.setId(Instant.now().getEpochSecond());
    avgBlockTimeDto.setType("distribution_data");
    avgBlockTimeDto.setAverageBlockTime(avgBlockTimes);
    redissonClient.getBucket(CACHE_KEY)
        .set(JsonUtil.toJSONString(avgBlockTimeDto), Duration.ofMinutes(TTL_MINUTES));

    log.info("Average block time cache generated successfully");

  }
}