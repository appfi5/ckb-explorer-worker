package com.ckb.explore.worker.utils;

import static com.ckb.explore.worker.constants.CommonConstantsKey.SECONDS_IN_DAY;

public class DateUtils {

  /**
   * 获取当天的开始时间（毫秒级）
   */
  public static Long getStartedAt(Long dateTime) {
    return dateTime == null ? null : dateTime * 1000;
  }

  /**
   * 获取当天的结束时间（毫秒级）
   */
  public static Long getEndedAt(Long dateTime) {
    return dateTime == null ? null : (dateTime + SECONDS_IN_DAY) * 1000;
  }
}
