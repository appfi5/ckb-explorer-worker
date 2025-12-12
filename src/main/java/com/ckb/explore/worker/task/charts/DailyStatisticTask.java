package com.ckb.explore.worker.task.charts;

import static com.ckb.explore.worker.constants.CommonConstantsKey.GENESIS_TIMESTAMP;
import static com.ckb.explore.worker.constants.CommonConstantsKey.SECONDS_IN_DAY;
import static com.ckb.explore.worker.constants.CommonConstantsKey.TESTNET_GENESIS_TIMESTAMP;
import static com.ckb.explore.worker.constants.RedisConstantsKey.DAILY_LAST_PROCESSED_KEY;

import com.ckb.explore.worker.service.BlockService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;
import com.ckb.explore.worker.service.DailyStatisticsService;

/**
 * DailyStatisticTask 每日统计任务
 * 对应Ruby的Charts::DailyStatistic类
 */
@Component
@Slf4j
public class DailyStatisticTask {

  @Resource
  RedissonClient redissonClient;

  @Resource
  DailyStatisticsService dailyStatisticsService;

  @Value("${ckb.netWork}")
  private Integer netWork;

  @Resource
  private BlockService blockService;

    // 定时执行，默认每天凌晨1点执行
    @Scheduled(cron = "${cron.dailyStatistic:0 0 1 * * ?}")
    public void perform() {
        perform(null);
    }

    /**
     * 执行每日统计任务
     * @param startDate 可选的日期时间参数
     */
    public void perform(LocalDate startDate) {

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
            log.info("从指定日期 {} 开始重算统计数据", startDate);
          } else {
            // 优先取redis的最近记录时间戳（秒）
            RBucket<String> bucket = redissonClient.getBucket(DAILY_LAST_PROCESSED_KEY);
            Long lastCreatedAtUnix = bucket.get() == null ? null : Long.parseLong(bucket.get());

            // redis里没有记录，则取表里的最大时间戳
            if(lastCreatedAtUnix == null){
              lastCreatedAtUnix = dailyStatisticsService.getMaxCreatAt();
            }

            if (lastCreatedAtUnix != null) {
              // 如果有记录，则从最后一条记录的后一天开始
              startDateTime = lastCreatedAtUnix + SECONDS_IN_DAY;
              log.info("从最后处理日期 {} 的下一天开始统计", lastCreatedAtUnix);
            } else {
              // 如果redis跟表里都没有记录，则从创世区块所在当天0点开始 毫秒转秒
              Long genesisBlockTimestamp = 
                  netWork == 1 ? GENESIS_TIMESTAMP : TESTNET_GENESIS_TIMESTAMP;
              // 转成Unix 时间的当天0点0分0秒时间戳
              startDateTime = Instant.ofEpochMilli(genesisBlockTimestamp).atZone(ZoneOffset.UTC)
                  .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond();
              log.info("首次运行，从创世区块日期 {} 开始统计", startDateTime);
            }
          }

          // 只处理需要的日期范围
          if (startDateTime < endDateTime) {
            dailyStatisticsService.statistic(startDateTime, endDateTime);
          } else {
            log.info("没有需要处理的日期");
          }
        } catch (Exception e) {
            log.error("Error in dailyStatisticTask", e);
        }
    }
}