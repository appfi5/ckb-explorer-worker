package com.ckb.explore.worker;

import com.ckb.explore.worker.service.BlockStatisticsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@Slf4j
class CkbExploreWorkerApplicationTests {

    @Resource
    BlockStatisticsService blockStatisticsService;
    @Resource
    RedissonClient redissonClient;

    @Test
    void contextLoads(){

    }
    @Test
    void blockStatisticTaskTest() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(5);

        for(int i=0;i<5;i++){
            new Thread(() -> {
                RLock lock = redissonClient.getLock("blockStatisticTaskTest");
                try {
                    Boolean isLock = lock.tryLock(1, 30, TimeUnit.SECONDS);
                    if (isLock) {
                       log.info("blockNumber is {}", blockStatisticsService.getMaxBlockNumber());
                     }
                }catch (Exception e){
                     e.printStackTrace();
                }finally {
                    lock.unlock();
                }

                countDownLatch.countDown();

            }).start();

        }
        countDownLatch.await();



    }






}
