package com.ckb.explore.worker;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ckb.explore.worker.domain.dto.BlockDaoDto;
import com.ckb.explore.worker.domain.dto.DaoCellDto;
import com.ckb.explore.worker.mapper.DepositCellMapper;
import com.ckb.explore.worker.mapper.WithdrawCellMapper;
import com.ckb.explore.worker.service.BlockService;
import com.ckb.explore.worker.service.DailyStatisticsService;
import com.ckb.explore.worker.utils.CkbUtil;
import com.ckb.explore.worker.utils.DaoCompensationCalculator;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class DailyStatisticsServiceImplTests {

  @Resource
  private WithdrawCellMapper withdrawCellMapper;

  @Resource
  private BlockService blockService;

  @Resource
  private DepositCellMapper depositCellMapper;

  @Resource
  private DailyStatisticsService dailyStatisticsService;


  private static final BigDecimal MILLISECONDS_IN_DAY = BigDecimal.valueOf(24 * 60 * 60 * 1000L);
  @Test
  public void calculatePhase1DaoInterests(){
    Long startedAt = 1630713600000L;
    Long endedAt =   1630800000000L;
    var commonStatisticInfo = blockService.getCommonStatisticInfo(startedAt, endedAt);
    Page<DaoCellDto> pageable = new Page<>(1, 10000);
    Page<DaoCellDto> nervosDaoWithdrawingCellsPage = withdrawCellMapper.getUnConsumedCellsByEndTime(pageable, endedAt);
    var phase1DaoInterests = calculateDaoInterestsSum(
        nervosDaoWithdrawingCellsPage.getRecords());
    var unmadeDaoInterests = calculateUnmadeDaoInterests(endedAt,commonStatisticInfo.getMaxBlockNumber());
    BigInteger unclaimedCompensation =phase1DaoInterests.add(unmadeDaoInterests);
    log.info("unclaimedCompensation:"+unclaimedCompensation);

    // startedAt = 1635984000000L; endedAt =1636070400000L; unclaimedCompensation 198492401911452
  }

  private BigInteger calculateDaoInterestsSum(List<DaoCellDto> cells) {
    BigInteger total = BigInteger.ZERO;
//    BigDecimal sumInterestBearing = BigDecimal.ZERO;
//    BigInteger interestBearingDeposits = BigInteger.ZERO;
//    DaoInterestsSumWithTimeDto result = new DaoInterestsSumWithTimeDto();
    Set<Long> depositBlockNumbers = cells.stream()
        .map(withdrawCell -> CkbUtil.convertToBlockNumber(withdrawCell.getData())).collect(
            Collectors.toSet());
    Set<Long> withdrawBlockNumbers = cells.stream().map(DaoCellDto::getBlockNumber)
        .collect(Collectors.toSet());
    depositBlockNumbers.addAll(withdrawBlockNumbers);
    // 获取所有相关区块的DAO信息
    List<BlockDaoDto> blockWithBlockTimestamp = blockService.getBlockDaoWithBlockTimestamp(depositBlockNumbers);

    Map<Long, byte[]> blockDaos = blockWithBlockTimestamp.stream()
        .collect(Collectors.toMap(BlockDaoDto::getBlockNumber, BlockDaoDto::getDao));
    Map<Long, Long> blockTimestamps = blockWithBlockTimestamp.stream()
        .collect(Collectors.toMap(BlockDaoDto::getBlockNumber, BlockDaoDto::getTimestamp));
    for (DaoCellDto withdrawCell : cells) {
      total = total.add(
          DaoCompensationCalculator.call(withdrawCell,
              blockDaos.get(withdrawCell.getBlockNumber()),
              blockDaos.get(CkbUtil.convertToBlockNumber(withdrawCell.getData()))));
//      long timeDiffMs = withdrawCell.getBlockTimestamp() - blockTimestamps.get(CkbUtil.convertToBlockNumber(withdrawCell.getData()));
//      var days = BigDecimal.valueOf(timeDiffMs).divide( // 存款时间总和 单位天
//          MILLISECONDS_IN_DAY,
//          6, // 保留6位小数，避免中间计算误差
//          RoundingMode.HALF_UP
//      );
//      var capacity = withdrawCell.getValue(); // 存款金额
//      sumInterestBearing  = sumInterestBearing.add(new BigDecimal(capacity).multiply(days)); // 时间贡献 = 存款金额 * 存款时间
//      interestBearingDeposits = interestBearingDeposits.add(capacity);
    }

    return total;
  }

  private BigInteger calculateUnmadeDaoInterests(Long endedAt, Long maxBlockNumber) {
    BigInteger total = BigInteger.ZERO;
//    BigInteger uninterestBearingDeposits = BigInteger.ZERO; // 无息存款总额
//    BigDecimal sumUninterestBearing = BigDecimal.ZERO; // 无息存款时间贡献总和
//    var result = new UnmadeDaoInterestsWithTimeDto();
    byte[] tipBlockDao = blockService.getDaoByBlockNumber(maxBlockNumber);
    if(tipBlockDao == null){
      return total;
    }

    // 分页查询：避免一次性加载大量数据（模拟Ruby的find_each）
    int page = 1;

      Page<DaoCellDto> pageable = new Page<>(page, 10000);
      Page<DaoCellDto> nervosDaoDepositCellsPage = depositCellMapper.getUnConsumedCellsByEndTime(pageable,
          endedAt);

      if (nervosDaoDepositCellsPage.getRecords().isEmpty()) {
        return total;
      }

      var nervosDaoDepositCells = nervosDaoDepositCellsPage.getRecords();
      Set<Long> depositBlockNumbers = nervosDaoDepositCells.stream().map(DaoCellDto::getBlockNumber)
          .collect(Collectors.toSet());
      Map<Long, byte[]> blockDaos = blockService.getBlockDaos(depositBlockNumbers);

      // 使用CkbUtils计算每个单元格的DAO利息
      for (DaoCellDto cell : nervosDaoDepositCells) {
        total = total.add(DaoCompensationCalculator.call(cell, tipBlockDao,
            blockDaos.get(cell.getBlockNumber())));
//        var timeDiffMs = endedAt - cell.getBlockTimestamp();
//        var days = BigDecimal.valueOf(timeDiffMs).divide( // 存款时间总和 单位天
//            MILLISECONDS_IN_DAY,
//            6, // 保留6位小数，避免中间计算误差
//            RoundingMode.HALF_UP
//        );
//        var capacity = cell.getValue(); // 存款金额
//        sumUninterestBearing  = sumUninterestBearing.add(new BigDecimal(capacity).multiply(days)); // 时间贡献 = 存款金额 * 存款时间
//        uninterestBearingDeposits = uninterestBearingDeposits.add(capacity);
      }


//    result.setTotalUnmadeDaoInterests(total);
//    result.setUninterestBearingDeposits(uninterestBearingDeposits);
//    result.setSumUninterestBearing(sumUninterestBearing);
    return total;
  }

  @Test
  public void testReSetMiningReward(){
    dailyStatisticsService.reSetMiningReward();
    log.info("ReSetMiningReward");
  }

  @Test
  public void testReSetOccupiedCapacityWithHolderCount(){
    dailyStatisticsService.reSetOccupiedCapacityWithHolderCount();
    log.info("ReSetOccupiedCapacityWithHolderCount");
  }

}
