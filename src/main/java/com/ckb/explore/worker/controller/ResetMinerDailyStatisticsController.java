package com.ckb.explore.worker.controller;

import static com.ckb.explore.worker.constants.CommonConstantsKey.SECONDS_IN_DAY;
import static com.ckb.explore.worker.constants.RedisConstantsKey.MINER_LAST_PROCESSED_KEY;

import com.ckb.explore.worker.service.DailyStatisticsService;
import com.ckb.explore.worker.service.MinerDailyStatisticsService;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/miner_daily_statistics")
@Slf4j
public class ResetMinerDailyStatisticsController {

  @Resource
  MinerDailyStatisticsService minerDailyStatisticService;

  @Resource
  RedissonClient redissonClient;

  @Resource
  DailyStatisticsService dailyStatisticsService;

  /**
   * 手动触发udt每日统计任务
   *
   * @param startDate 可选的日期时间参数
   */
  @PostMapping("/manual_trigger")
  public Boolean manualTrigger(@RequestParam(required = false) LocalDate startDate) {

    try {

      // endDateTime为最新的每日统计时间，一般为最新块的前一天
      Long endDateTime = dailyStatisticsService.getMaxCreatAt();

      // 开始时间支持传入，方便更新
      Long startDateTime;

      if (startDate != null) {
        startDateTime = startDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .getEpochSecond();
      } else {
        // 优先取redis的最近记录时间戳（秒）
        RBucket<String> bucket = redissonClient.getBucket(MINER_LAST_PROCESSED_KEY);
        Long lastCreatedAtUnix = bucket.get() == null ? null : Long.parseLong(bucket.get());

        // redis里没有记录，则取表里的最大时间戳
        if (lastCreatedAtUnix == null) {
          lastCreatedAtUnix = minerDailyStatisticService.getMaxCreatAt();
        }

        if (lastCreatedAtUnix != null) {
          // 如果有记录，则从最后一条记录的后一天开始
          startDateTime = lastCreatedAtUnix + SECONDS_IN_DAY;
        } else {
          // 如果没有记录，则取每日统计表的第一条记录的时间戳
          startDateTime = dailyStatisticsService.getMinCreatAt();
        }
      }

      // 只处理需要的日期范围
      if (startDateTime < endDateTime) {
        minerDailyStatisticService.perform(startDateTime, endDateTime);
      } else {
        log.info("MinerDailyStatisticTask 没有需要处理的日期");
      }
    } catch (Exception e) {
      log.error("Error in MinerDailyStatisticTask", e);
      return false;
    }
    return true;
  }
}
