package com.ckb.explore.worker.task.charts;

import static com.ckb.explore.worker.constants.RedisConstantsKey.LOCK_WAIT_TIME;

import com.ckb.explore.worker.domain.dto.EpochInfoDto;
import com.ckb.explore.worker.entity.EpochStatistic;
import com.ckb.explore.worker.service.BlockService;
import com.ckb.explore.worker.utils.AutoReleaseRLock;
import com.ckb.explore.worker.utils.TypeConversionUtil;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;
import com.ckb.explore.worker.service.EpochStatisticService;

import java.util.concurrent.TimeUnit;

/**
 * EpochStatistic Epoch统计任务 每30分钟执行一次，处理Epoch相关统计
 */
@Component
@Slf4j
public class EpochStatisticTask {

  private static final String LOCK_NAME = "epochStatistic";

  @Resource
  private RedissonClient redissonClient;

  @Resource
  private EpochStatisticService epochStatisticService;

  @Resource
  private BlockService blockService;

  // 每30分钟执行一次，使用cron表达式确保任务不重叠
  @Scheduled(cron = "0 */30 * * * ?")
  public void perform() throws InterruptedException {
    RLock lock = redissonClient.getLock(LOCK_NAME);
    try(AutoReleaseRLock autoReleaseRLock = new AutoReleaseRLock(lock, LOCK_WAIT_TIME, TimeUnit.SECONDS))  {

      // 获取最新的Epoch编号
      Long latestEpochNumber = epochStatisticService.getLatestEpochNumber();

      // 当前表里的最大Epoch编号
      Long maxEpochNumber = blockService.getMaxEpochNumber();

      for(Long targetEpochNumber = latestEpochNumber == null? 0: latestEpochNumber + 1; targetEpochNumber <= maxEpochNumber; targetEpochNumber++){
        // 执行Epoch统计生成
        log.info( "EpochStatistic start targetEpochNumber is {}", targetEpochNumber);
        new EpochStatisticGenerator(targetEpochNumber).call();
      }

    } catch (Exception e) {
      log.error("Error in EpochStatistic", e);
    }
  }

  /**
   * EpochStatisticGenerator Epoch统计生成器 用于生成指定Epoch的统计数据
   */
  public class EpochStatisticGenerator {

    private final Long targetEpochNumber;

    public EpochStatisticGenerator(Long targetEpochNumber) {
      this.targetEpochNumber = targetEpochNumber;
    }

    /**
     * 执行Epoch统计
     */
    public void call() {
      // 查找指定Epoch的信息
      EpochInfoDto epochInfo = blockService.findEpochInfoByTargetEpochNumber(targetEpochNumber);
      if (epochInfo == null) {
        log.info("No block found in epoch: {}", targetEpochNumber);
        return;
      }

      // 检查该Epoch的区块数量是否完整
      if (epochInfo.getBlocksCount().intValue() != epochInfo.getEpochLength().intValue()) {
        log.info("Blocks count {} not match epoch length {} for epoch: {}",
            epochInfo.getBlocksCount(), epochInfo.getEpochLength(), targetEpochNumber);
        return;
      }

      // 查指定Epoch统计记录
      EpochStatistic epochStatistic =
          epochStatisticService.findByEpochNumber(targetEpochNumber);
      if (epochStatistic == null) {
        // 如果不存在，则创建新的统计记录
        epochStatistic = new EpochStatistic();
      }

      epochStatistic.setEpochNumber(targetEpochNumber);
      var difficulty = TypeConversionUtil.byteConvertToUInt256(epochInfo.getDifficulty());
      var totalBlocks = epochInfo.getBlocksCount();
      var totalUncles = epochInfo.getUnclesCount();
      var totalBlockTime = epochInfo.getMaxTimestamp() - epochInfo.getMinTimestamp();
      epochStatistic.setDifficulty(difficulty.toString());
      epochStatistic.setUncleRate(
          totalBlocks > 0 ? new BigDecimal(totalUncles).divide(new BigDecimal(totalBlocks), 5,
              RoundingMode.FLOOR).stripTrailingZeros() : BigDecimal.ZERO);
      epochStatistic.setHashRate(
          totalBlockTime > 0 ? new BigDecimal(difficulty).multiply(new BigDecimal(epochInfo.getEpochLength())).divide(new BigDecimal(totalBlockTime), 0,
              RoundingMode.FLOOR).toString() : BigDecimal.ZERO.toString());
      epochStatistic.setEpochTime(totalBlockTime);
      epochStatistic.setEpochLength(epochInfo.getEpochLength());
      epochStatistic.setLargestTxHash(epochInfo.getLargestTxHash());
      epochStatistic.setLargestTxBytes(epochInfo.getLargestTxBytes());
      epochStatistic.setMaxTxCycles(epochInfo.getMaxTxCycles());
      epochStatistic.setMaxBlockCycles(epochInfo.getMaxBlockCycles());
      epochStatistic.setLargestBlockNumber(epochInfo.getLargestBlockNumber());// size最大块的number
      epochStatistic.setLargestBlockSize(epochInfo.getLargestBlockSize());

      // 保存或重置Epoch统计的各个字段
      epochStatisticService.saveOrUpdate(epochStatistic);
    }
  }

  /**
   * 启动时运行一次，补足历史数据
   */
  @PostConstruct
  public void initExecute() {
    log.info("项目启动，立即执行一次平均区块时间任务");
    try {
      perform();
    } catch (Exception e) {
      log.error("项目启动时执行任务失败", e);
    }
  }
}