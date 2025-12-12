package com.ckb.explore.worker.domain.dto;

import com.ckb.explore.worker.utils.CkbUtil.DaoDto;
import java.math.BigDecimal;
import java.math.BigInteger;
import lombok.Data;

@Data
public class MarketDataDto {
  private BigInteger ecosystemLocked;
  private BigInteger teamLocked;
  private BigInteger privateSaleLocked;
  private BigInteger foundingPartnersLocked;
  private BigInteger foundationReserveLocked;
  private BigInteger bugBountyLocked;
  private BigDecimal circulatingSupply;
  private DaoDto parsedDao;
}
