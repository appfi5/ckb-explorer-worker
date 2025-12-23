package com.ckb.explore.worker.controller;

import static com.ckb.explore.worker.constants.CommonConstantsKey.GENESIS_TIMESTAMP;
import static com.ckb.explore.worker.constants.CommonConstantsKey.SECONDS_IN_DAY;
import static com.ckb.explore.worker.constants.CommonConstantsKey.TESTNET_GENESIS_TIMESTAMP;

import com.ckb.explore.worker.service.BlockService;
import com.ckb.explore.worker.service.DailyStatisticsService;
import jakarta.annotation.Resource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/daily_statistics")
@Slf4j
public class ResetDailyStatisticsController {

  private static final String LAST_PROCESSED_KEY = "daily_last_processed_date";

  @Resource
  DailyStatisticsService dailyStatisticsService;

  @Resource
  RedissonClient redissonClient;

  @Resource
  BlockService blockService;

  @Value("${ckb.netWork}")
  private Integer netWork;

  @PostMapping("/reset")
  public Boolean reset(@RequestParam String field) {

    if (field != null && !field.isEmpty()) {
      switch (field) {
        case "mining_reward":
          log.info("reset mining_reward");
          dailyStatisticsService.reSetMiningReward();
          break;
        case "occupied_capacity":
        case "holder_count":
          log.info("reset occupied_capacity holder_count");
          dailyStatisticsService.reSetOccupiedCapacityWithHolderCount();
          break;
        case "treasury_amount":
          log.info("reset treasury_amount");
          dailyStatisticsService.reSetTreasuryAmount();
          break;

      }
    }

    return true;
  }

  /**
   * 手动触发每日统计任务
   *
   * @param startDate 可选的日期时间参数
   */
  @PostMapping("/manual_trigger")
  public Boolean manualTrigger(@RequestParam(required = false) LocalDate startDate) {

    log.info("手动触发每日统计,指定开始日期{}", startDate);

    try {
      // 处理日期参数，结束时间设置为最新块的当天的开始时间，即最多统计到最新块的前一天
      var maxTimestamp = blockService.getMaxTimestamp();
      Long endDateTime = Instant.ofEpochMilli(maxTimestamp)
          .atZone(ZoneOffset.UTC)
          .toLocalDate()
          .atStartOfDay(ZoneOffset.UTC)
          .toInstant()
          .getEpochSecond();

      Long startDateTime;

      if (startDate != null) {
        startDateTime = startDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .getEpochSecond();
        // 从指定日期开始重算
        log.info("手动触发每日统计,从指定日期 {} 开始重算统计数据", startDate);
      } else {
        // 优先取redis的最近记录时间戳（秒）
        RBucket<String> bucket = redissonClient.getBucket(LAST_PROCESSED_KEY);
        Long lastCreatedAtUnix = bucket.get() == null ? null : Long.parseLong(bucket.get());

        // redis里没有记录，则取表里的最大时间戳
        if (lastCreatedAtUnix == null) {
          lastCreatedAtUnix = dailyStatisticsService.getMaxCreatAt();
        }

        if (lastCreatedAtUnix != null) {
          // 如果有记录，则从最后一条记录的后一天开始
          startDateTime = lastCreatedAtUnix + SECONDS_IN_DAY;
          log.info("手动触发每日统计,从最后处理日期 {} 的下一天开始统计", lastCreatedAtUnix);
        } else {
          // 如果redis跟表里都没有记录，则从创世区块所在当天0点开始 毫秒转秒
          Long genesisBlockTimestamp =
              netWork == 1 ? GENESIS_TIMESTAMP : TESTNET_GENESIS_TIMESTAMP;
          // 转成Unix 时间的当天0点0分0秒时间戳
          startDateTime = Instant.ofEpochMilli(genesisBlockTimestamp).atZone(ZoneOffset.UTC)
              .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond();
          log.info("手动触发每日统计,首次运行，从创世区块日期 {} 开始统计", startDateTime);
        }
      }

      // 只处理需要的日期范围
      if (startDateTime < endDateTime) {
        // 异步调用，不用等待处理结果
        dailyStatisticsService.statistic(startDateTime, endDateTime);
      } else {
        log.info("手动触发每日统计,没有需要处理的日期");
      }
    } catch (Exception e) {
      log.error("Error in dailyStatisticTask", e);
    }
    return true;
  }
}
