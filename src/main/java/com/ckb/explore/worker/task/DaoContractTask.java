package com.ckb.explore.worker.task;

import static com.ckb.explore.worker.constants.RedisConstantsKey.LOCK_WAIT_TIME;

import com.ckb.explore.worker.service.DaoContractsService;
import com.ckb.explore.worker.utils.AutoReleaseRLock;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

/**
 * DAO合约未认领补偿金额计算任务
 * 每小时执行一次，计算并更新默认DAO合约的未认领补偿金额
 */
@Component
@Slf4j
public class DaoContractTask {

    private static final String LOCK_NAME = "daoContractTask";

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private DaoContractsService daoContractsService;

    /**
     * 每小时执行一次，使用cron表达式确保任务不重叠
     */
    @Scheduled(cron = "${cron.daoContract:0 0 * * * ?}")
    public void perform(){
        RLock lock = redissonClient.getLock(LOCK_NAME);

        try(AutoReleaseRLock autoReleaseRLock = new AutoReleaseRLock(lock, LOCK_WAIT_TIME, TimeUnit.SECONDS))  {

            // 获取默认DAO合约并更新
          daoContractsService.perform();

        } catch (Exception e) {
            log.error("Error in daoContractTask", e);
        }
    }
}