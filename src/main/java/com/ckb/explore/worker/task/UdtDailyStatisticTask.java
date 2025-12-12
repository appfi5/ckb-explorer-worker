package com.ckb.explore.worker.task;

import static com.ckb.explore.worker.constants.CommonConstantsKey.GENESIS_TIMESTAMP;
import static com.ckb.explore.worker.constants.CommonConstantsKey.SECONDS_IN_DAY;
import static com.ckb.explore.worker.constants.CommonConstantsKey.TESTNET_GENESIS_TIMESTAMP;
import static com.ckb.explore.worker.constants.RedisConstantsKey.UDT_LAST_PROCESSED_KEY;

import com.ckb.explore.worker.service.BlockService;
import com.ckb.explore.worker.service.UdtDailyStatisticsService;
import jakarta.annotation.Resource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UdtDailyStatisticTask {

  @Resource
  UdtDailyStatisticsService udtDailyStatisticService;

  @Resource
  RedissonClient redissonClient;

  @Resource
  private BlockService blockService;

  @Value("${ckb.netWork}")
  private Integer netWork;

  /**
   * 每天1点执行统计任务
   * cron 每天1点0分执行
   * zone 指定时区
   */
  @Scheduled(cron = "${cron.dailyStatistic:0 0 1 * * ?}")
  public void scheduleUdtDailyStatistic() throws InterruptedException {
    // 调用worker的perform方法，原代码中datetime默认传null
    perform(null);
  }

  /**
   * 执行每日统计任务
   * @param startDate 统计开始时间，默认为null，表示从创世区块开始
   */
  public void perform(LocalDate startDate ) throws InterruptedException {
    try {

      // endDateTime为最新块的当天的开始时间，即最多统计到最新块的前一天
      var maxTimestamp = blockService.getMaxTimestamp();
      Long endDateTime = Instant.ofEpochMilli(maxTimestamp)
          .atZone(ZoneOffset.UTC)
          .toLocalDate()
          .atStartOfDay(ZoneOffset.UTC)
          .toInstant()
          .getEpochSecond();

      // 开始时间支持传入，方便更新
      Long startDateTime;

      if (startDate != null) {
        startDateTime = startDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .getEpochSecond();
      } else {
        // 优先取redis的最近记录时间戳（秒）
        RBucket<String> bucket = redissonClient.getBucket(UDT_LAST_PROCESSED_KEY);
        Long lastCreatedAtUnix = bucket.get() == null ? null : Long.parseLong(bucket.get());

        // redis里没有记录，则取表里的最大时间戳
        if (lastCreatedAtUnix == null) {
          lastCreatedAtUnix = udtDailyStatisticService.getMaxCreatAt();
        }

        if (lastCreatedAtUnix != null) {
          // 如果有记录，则从最后一条记录的后一天开始
          startDateTime = lastCreatedAtUnix + SECONDS_IN_DAY;
        } else {
          // 如果没有记录，则从创世区块所在当天0点开始 毫秒转秒
          Long genesisBlockTimestamp = netWork == 1 ? GENESIS_TIMESTAMP : TESTNET_GENESIS_TIMESTAMP;
          // 转成Unix 时间的当天0点0分0秒时间戳 .atZone(ZoneOffset.UTC)..atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond()
          startDateTime = Instant.ofEpochMilli(genesisBlockTimestamp).atZone(ZoneOffset.UTC)
              .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond();
        }
      }

      // 只处理需要的日期范围
      if (startDateTime < endDateTime) {
        udtDailyStatisticService.perform(startDateTime, endDateTime);
      } else {
        log.info("没有需要处理的日期");
      }
    } catch (Exception e) {
      log.error("Error in udtDailyStatisticTask", e);
      throw e;
    }
  }
}
