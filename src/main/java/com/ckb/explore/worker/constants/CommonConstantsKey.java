package com.ckb.explore.worker.constants;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface CommonConstantsKey {

  Long SECONDS_IN_DAY = (long) (24 * 60 * 60); // 一天的秒数

  long GENESIS_TIMESTAMP = 1573852190812L; // 主网创世时间戳（毫秒）

  long TESTNET_GENESIS_TIMESTAMP = 1589276230000L; // 测试网创世时间戳

  String ZERO_LOCK_CODE_HASH = "0x0000000000000000000000000000000000000000000000000000000000000000";

  String NO_DATA_HASH = "0x";

  BigDecimal SHANNON_TO_CKB = BigDecimal.valueOf(100_000_000);

  BigDecimal MILLISECONDS_IN_DAY = BigDecimal.valueOf(24 * 60 * 60 * 1000L);

  // BURN_QUOTA = 84 * 10^16
  BigInteger BURN_QUOTA = new BigInteger("84").multiply(new BigInteger("10").pow(16));

  BigDecimal SECONDARY_EPOCH_REWARD = BigDecimal.valueOf(613_698_63013698L);

  // INITIAL_SUPPLY = 336 * 10^16
  BigInteger INITIAL_SUPPLY = new BigInteger("336").multiply(new BigInteger("10").pow(16));

  // 各配额计算（基于INITIAL_SUPPLY的百分比）
  BigInteger ECOSYSTEM_QUOTA = INITIAL_SUPPLY.multiply(BigInteger.valueOf(17)).divide(BigInteger.valueOf(100));   // 先乘17（整数运算，避免小数）再除以100（整除，结果为整数）
  BigInteger TEAM_QUOTA = INITIAL_SUPPLY.multiply(BigInteger.valueOf(15)).divide(BigInteger.valueOf(100));
  BigInteger PRIVATE_SALE_QUOTA = INITIAL_SUPPLY.multiply(BigInteger.valueOf(14)).divide(BigInteger.valueOf(100));
  BigInteger FOUNDING_PARTNER_QUOTA = INITIAL_SUPPLY.multiply(BigInteger.valueOf(5)).divide(BigInteger.valueOf(100));
  BigInteger FOUNDATION_RESERVE_QUOTA = INITIAL_SUPPLY.multiply(BigInteger.valueOf(2)).divide(BigInteger.valueOf(100));
  // 默认单位
  String DEFAULT_UNIT = "ckb";
  String BOUNTY_ADDRESS_HASH = "ckb1qyqy6mtud5sgctjwgg6gydd0ea05mr339lnslczzrc";

}
