package com.ckb.explore.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableScheduling
public class ThreadPoolConfigure {

  @Bean("threadPoolTaskExecutor")
  public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
    int processors = Runtime.getRuntime().availableProcessors();
    ThreadPoolTaskExecutor threadPool = new ThreadPoolTaskExecutor();
    threadPool.setCorePoolSize(2 * processors + 1);
    threadPool.setMaxPoolSize(2 * processors + 1);
    threadPool.setQueueCapacity(10000);
    threadPool.setKeepAliveSeconds(60);
    threadPool.setThreadGroupName("ckb-explore-");
    threadPool.setThreadNamePrefix("worker-");
    threadPool.setWaitForTasksToCompleteOnShutdown(true);
    threadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    threadPool.setAwaitTerminationSeconds(10);
    return threadPool;
  }

  @Bean
  public ScheduledExecutorService schedulerPool() {
    return new ScheduledThreadPoolExecutor(8);
  }



}
