package com.ckb.explore.worker.config;

import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@Slf4j
public class AsyncConfig {

  @Bean(name = "asyncStatisticTaskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5); // 核心线程数
    executor.setMaxPoolSize(10); // 最大线程数
    executor.setQueueCapacity(50); // 队列容量
    executor.setThreadNamePrefix("Recompute-"); // 线程名前缀（便于日志追踪）
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 拒绝策略（避免任务丢失）
    // 设置异步任务超时时间（5分钟，单位毫秒）
    executor.setTaskDecorator(runnable -> {
      return () -> {
        try {
          // 使用Future.get()设置超时，替代全局超时
          FutureTask<?> future = new FutureTask<>(runnable, null);
          future.run();
          future.get(300000, TimeUnit.MILLISECONDS); // 5分钟超时
        } catch (Exception e) {
          Thread.currentThread().interrupt(); // 恢复中断状态
          throw new RuntimeException("异步任务超时或中断", e);
        }
      };
    });
    executor.initialize();
    log.info("asyncStatisticTaskExecutor 线程池初始化完成");
    return executor;
  }
}
