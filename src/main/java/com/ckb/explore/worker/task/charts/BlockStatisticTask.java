package com.ckb.explore.worker.task.charts;

import com.ckb.explore.worker.entity.BlockStatistics;
import com.ckb.explore.worker.service.BlockStatisticsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class BlockStatisticTask {

    @Resource
    RedissonClient redissonClient;

    @Resource
    BlockStatisticsService blockStatisticsService;

    //默认每10分钟执行一次
    @Scheduled(cron = "${cron.blockStatistic:* */10 * * * *}")
    public void  blockStatisticTask() throws InterruptedException {
        RLock lock = redissonClient.getLock("blockStatisticTask");
        Boolean isLock;
        try {
            isLock = lock.tryLock(5,60, TimeUnit.SECONDS);
            log.info("blockStatisticTask start isLock is {}",isLock);
            if(!isLock){
                return;
            }
            // 1.  获取最新已统计的区块号：从block_statistics表中查询最大的number，若表为空则默认为0
            Long latestBlockNumber = blockStatisticsService.getMaxBlockNumber();
            // 2. 计算目标统计区块号：在最新已统计区块号的基础上加100（每次处理100个区块后的下一个目标）
            Long targetBlockNumber = latestBlockNumber+100;
            // 3. 查找目标区块号对应的区块记录（从Block表中）

            // 4. 若目标区块不存在，或该区块的统计记录已存在，则直接返回（不执行后续统计）

            // 5. 按照目标块高生成最新的 stat_block 大小是block表的1%
        }catch (Exception e){

        }finally {
            lock.unlock();
        }

    }
}
