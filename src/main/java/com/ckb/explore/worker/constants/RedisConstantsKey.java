package com.ckb.explore.worker.constants;

public interface RedisConstantsKey {

     String UDT_HOLDER_ALLOCATION = "UDT_HOLDER_ALLOCATION";

     Long LOCK_WAIT_TIME = 5L;

  String MINER_LAST_PROCESSED_KEY = "miner_last_processed_date";

  String MINER_STATISTIC_LOCK_KEY = "miner_statistic_global_lock";

  String MINER_STATISTIC_DATE_LOCK_KEY = "miner_statistic_date_lock_";

  String UDT_LAST_PROCESSED_KEY = "udt_last_processed_date";

  String UDT_STATISTIC_LOCK_KEY = "udt_statistic_global_lock";

  String UDT_STATISTIC_DATE_LOCK_KEY = "udt_statistic_date_lock_";

  String DAILY_LAST_PROCESSED_KEY = "daily_last_processed_date";

  String DAILY_STATISTIC_LOCK_KEY = "daily_statistic_global_lock";

  String DAILY_STATISTIC_DATE_LOCK_KEY = "daily_statistic_date_lock_";

  String DAILY_STATISTIC_RESET_LOCK_KEY = "daily_statistic_reset_lock";

  String DAILY_LAST_LOCK_OCCUPIED_KEY = "daily_last_lock_occupied_";
}
