package com.ckb.explore.worker.utils;

import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;

public class AutoReleaseRLock implements AutoCloseable {

  private final RLock lock;

  /**
   * 构造方法：获取锁并立即锁定（无超时，默认30秒看门狗续期）
   * @param lock Redisson 的 RLock 实例
   */
  public AutoReleaseRLock(RLock lock) {
    this.lock = lock;
    this.lock.lock(); // 锁定（阻塞直到获取锁）
  }

  /**
   * 构造方法2：带超时时间的锁定（指定自动释放时间，不启用看门狗）
   * 行为：最多等待waitTime，抢到锁后保持leaseTime时间后自动释放
   */
  public AutoReleaseRLock(RLock lock, long waitTime, long leaseTime, TimeUnit unit) {
    this.lock = lock;
    boolean locked;
    try {
      locked = lock.tryLock(waitTime, leaseTime, unit);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt(); // 恢复中断状态
      throw new IllegalStateException("获取锁时被中断", e);
    }
    if (!locked) {
      throw new IllegalStateException("超时未获取到锁（等待时间：" + waitTime + unit + "）");
    }
  }

  /**
   * 构造方法3：指定最大等待时间，抢到锁后启用看门狗
   * 行为：最多等待waitTime，抢到锁后启用看门狗自动续期
   */
  public AutoReleaseRLock(RLock lock, long waitTime, TimeUnit unit) {
    // 调用构造方法2，传入leaseTime = -1（启用看门狗）
    this(lock, waitTime, -1, unit);
  }

  /**
   * try-with-resources 代码块结束时自动调用此方法，释放锁
   */
  @Override
  public void close() {
    if (lock != null && lock.isHeldByCurrentThread()) {
      lock.unlock(); // 仅当当前线程持有锁时才释放，避免异常
    }
  }
}
