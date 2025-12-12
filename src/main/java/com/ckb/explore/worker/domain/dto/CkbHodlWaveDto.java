package com.ckb.explore.worker.domain.dto;

import java.math.BigInteger;
import lombok.Data;

@Data
public class CkbHodlWaveDto {

  private BigInteger overThreeYears;

  private BigInteger oneYearToThreeYears;

  private BigInteger sixMonthsToOneYear;

  private BigInteger threeMonthsToSixMonths;

  private BigInteger oneMonthToThreeMonths;

  private BigInteger oneWeekToOneMonth;

  private BigInteger dayToOneWeek;

  private BigInteger latestDay;
}
