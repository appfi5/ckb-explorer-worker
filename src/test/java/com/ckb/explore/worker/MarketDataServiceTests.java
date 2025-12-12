package com.ckb.explore.worker;

import com.ckb.explore.worker.mapper.DepositCellMapper;
import com.ckb.explore.worker.mapper.WithdrawCellMapper;
import com.ckb.explore.worker.service.BlockService;
import com.ckb.explore.worker.service.MarketDataService;
import jakarta.annotation.Resource;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class MarketDataServiceTests {

  @Resource
  MarketDataService marketDataService;

  @Resource
  BlockService blockService;

  @Resource
  DepositCellMapper depositCellMapper;

  @Resource
  WithdrawCellMapper withdrawCellMapper;

  @Test
  public void test() {
    //var maxBlockNumber = blockService.getMaxBlockNumberByTime(1621468800000L, 1621555200000L);

    var startAt = 1621440000* 1000L;
    var endedAt = startAt + 86400000L;

    var totalDaoDeposit = calculateTotalDaoDeposit(endedAt);
    log.info("totalDaoDeposit: {}", totalDaoDeposit);
    var maxBlockNumber = blockService.getMaxBlockNumberByTime(startAt, endedAt);
    var marketDataDto = marketDataService.getMarketData(maxBlockNumber, "shannon");
    log.info("CirculatingSupply {}", marketDataDto.getCirculatingSupply());
  }

  private BigInteger calculateTotalDaoDeposit(Long endedAt){
    BigInteger totalDeposit = depositCellMapper.totalDeposit(endedAt);
    totalDeposit = totalDeposit == null ? BigInteger.ZERO : totalDeposit;
    BigInteger totalWithdraw = withdrawCellMapper.totalWithdraw(endedAt);
    totalWithdraw = totalWithdraw == null ? BigInteger.ZERO : totalWithdraw;
    return totalDeposit.subtract(totalWithdraw);
  }
}
