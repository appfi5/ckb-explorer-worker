package com.ckb.explore.worker.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ckb.explore.worker.domain.dto.DaoCellDto;
import com.ckb.explore.worker.domain.dto.TotalDepositAndDepositorsCountDto;
import com.ckb.explore.worker.entity.DaoContracts;
import com.ckb.explore.worker.mapper.DailyStatisticsMapper;
import com.ckb.explore.worker.mapper.DaoContractsMapper;
import com.ckb.explore.worker.mapper.DepositCellMapper;
import com.ckb.explore.worker.mapper.WithdrawCellMapper;
import com.ckb.explore.worker.service.BlockService;
import com.ckb.explore.worker.service.DaoContractsService;
import com.ckb.explore.worker.utils.CkbUtil;
import com.ckb.explore.worker.utils.DaoCompensationCalculator;
import com.ckb.explore.worker.utils.JsonUtil;
import jakarta.annotation.Resource;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DaoContractsServiceImpl extends ServiceImpl<DaoContractsMapper, DaoContracts>
    implements DaoContractsService {

  @Resource
  private DepositCellMapper depositCellMapper;

  @Resource
  private WithdrawCellMapper withdrawCellMapper;

  @Resource
  private BlockService blockService;

  @Resource
  private DailyStatisticsMapper dailyStatisticsMapper;

  @Value("${dao.pageSize:1000}")
  private int daoPageSize;

  /**
   * 执行未认领补偿金额计算并更新
   */
  public void perform() {

    // 获取默认DAO合约
    DaoContracts defaultContract = getDefaultDaoContract();
    if (defaultContract == null) {
      defaultContract = new DaoContracts();
      defaultContract.setId(1L);
    }

    TotalDepositAndDepositorsCountDto totalDepositAndDepositorsCount = depositCellMapper.calTotalDepositAndDepositorsCount();
    // 更新
    defaultContract.setTotalDeposit(totalDepositAndDepositorsCount == null ? BigInteger.ZERO
        : totalDepositAndDepositorsCount.getTotalDeposit());
    defaultContract.setDepositorsCount(totalDepositAndDepositorsCount == null ? 0
        : totalDepositAndDepositorsCount.getDepositorsCount());
    // 计算已领取的利息
    defaultContract.setClaimedCompensation(calClaimedCompensation());
    // 计算未认领补偿金额(已申请未领取+未申请的利息)
    BigInteger unclaimedCompensation = calUnclaimedCompensation();
    defaultContract.setUnclaimedCompensation(unclaimedCompensation);
    this.saveOrUpdate(defaultContract);
    log.info("saveOrUpdate default DAO: {}", JsonUtil.toJSONString(defaultContract));

  }

  /**
   * 计算未认领补偿金额
   *
   * @return 未认领补偿金额
   */
  private BigInteger calUnclaimedCompensation() {
    return phase1DaoInterests().add(unmadeDaoInterests());
  }

  /**
   * 计算第一阶段DAO利息
   *
   * @return 第一阶段DAO利息总和
   */
  private BigInteger phase1DaoInterests() {
    BigInteger total = BigInteger.ZERO;
    try {
      // 查询未消耗的WithdrawCell
      // 分页查询：避免一次性加载大量数据（模拟Ruby的find_each）
      int page = 1;
      while (true) {
        Page pageable = new Page<>(page, daoPageSize);
        Page<DaoCellDto> nervosDaoWithdrawingCellsPage = withdrawCellMapper.getUnConsumedCells(pageable);
        if (nervosDaoWithdrawingCellsPage.getRecords().isEmpty()) {
          break;
        }
        var nervosDaoWithdrawingCells = nervosDaoWithdrawingCellsPage.getRecords();
        Set<Long> depositBlockNumbers = nervosDaoWithdrawingCells.stream()
            .map(withdrawCell -> CkbUtil.convertToBlockNumber(withdrawCell.getData()))
            .collect(Collectors.toSet());
        Set<Long> withdrawBlockNumbers = nervosDaoWithdrawingCells.stream()
            .map(DaoCellDto::getBlockNumber).collect(Collectors.toSet());
        depositBlockNumbers.addAll(withdrawBlockNumbers);

        Map<Long, byte[]> blockDaos = blockService.getBlockDaos(depositBlockNumbers);

        // 使用CkbUtils计算每个单元格的DAO利息
        for (DaoCellDto cell : nervosDaoWithdrawingCells) {
          total = total.add(DaoCompensationCalculator.call(cell,
              blockDaos.get(cell.getBlockNumber()),
              blockDaos.get(CkbUtil.convertToBlockNumber(cell.getData()))));
        }
        page++;
      }
    } catch (Exception e) {
      log.error("计算阶段1DAO利息异常", e);
    }
    return total;
  }

  /**
   * 计算未生成的DAO利息
   *
   * @return 未生成的DAO利息总和
   */
  private BigInteger unmadeDaoInterests() {
    BigInteger total = BigInteger.ZERO;
    try {
      Long maxBlockNumber = blockService.getMaxBlockNumber();
      byte[] tipBlockDao = blockService.getDaoByBlockNumber(maxBlockNumber);
      if(tipBlockDao == null){
        return total;
      }
      // 查询未消耗的depositCell
      // 分页查询：避免一次性加载大量数据（模拟Ruby的find_each）
      int page = 1;
      while (true) {
        Page pageable = new Page<>(page, daoPageSize);
        Page<DaoCellDto> nervosDaoDepositCellsPage = depositCellMapper.getUnConsumedCells(pageable);
        if (nervosDaoDepositCellsPage.getRecords().isEmpty()) {
          break;
        }
        var nervosDaoDepositCells = nervosDaoDepositCellsPage.getRecords();
        Set<Long> depositBlockNumbers = nervosDaoDepositCells.stream()
            .map(DaoCellDto::getBlockNumber)
            .collect(Collectors.toSet());

        Map<Long, byte[]> blockDaos = blockService.getBlockDaos(depositBlockNumbers);

        // 使用CkbUtils计算每个单元格的DAO利息
        for (DaoCellDto cell : nervosDaoDepositCells) {
          total = total.add(DaoCompensationCalculator.call(cell, tipBlockDao,
              blockDaos.get(cell.getBlockNumber())));
        }
        page++;
      }
    } catch (Exception e) {
      log.error("计算未生成的DAO利息异常", e);
    }
    return total;
  }

  /**
   * 获取默认DAO合约
   *
   * @return 默认DAO合约
   */
  private DaoContracts getDefaultDaoContract() {
    return baseMapper.selectById(1);
  }

  /**
   * 计算已领取的利息
   *
   * @return 已领取的利息总和
   */
  private BigInteger calClaimedCompensation() {
    BigInteger total = BigInteger.ZERO;
    try {
      // 上一次的统计
      var lastStatistics = dailyStatisticsMapper.getLastDayDailyStatistics();
      if (lastStatistics == null) {
        return total;
      }
      var laseDayStatisticsTime = lastStatistics.getBlockTimestamp();
      // 获取从上次统计后到现在已经发放利息的cell
      // 分页查询：避免一次性加载大量数据（模拟Ruby的find_each）
      int page = 1;
      while (true) {
        Page pageable = new Page<>(page, daoPageSize);
        var withdrawCellsPage = withdrawCellMapper.getConsumedCells(pageable,laseDayStatisticsTime);
        if (withdrawCellsPage.getRecords().isEmpty()) {
          break;
        }
        var withdrawCells = withdrawCellsPage.getRecords();
        Set<Long> depositBlockNumbers = withdrawCells.stream()
            .map(withdrawCell -> CkbUtil.convertToBlockNumber(withdrawCell.getData()))
            .collect(Collectors.toSet());
        Set<Long> withdrawBlockNumbers = withdrawCells.stream().map(DaoCellDto::getBlockNumber)
            .collect(Collectors.toSet());
        depositBlockNumbers.addAll(withdrawBlockNumbers);
        // 获取所有相关区块的DAO信息
        Map<Long, byte[]> blockDaos = blockService.getBlockDaos(depositBlockNumbers);

        for (DaoCellDto withdrawCell : withdrawCells) {
          total = total.add(
              DaoCompensationCalculator.call(withdrawCell,
                  blockDaos.get(withdrawCell.getBlockNumber()),
                  blockDaos.get(CkbUtil.convertToBlockNumber(withdrawCell.getData()))));
        }
        page++;
      }

      total = total.add(lastStatistics.getClaimedCompensation() == null ? BigInteger.ZERO: new BigInteger(lastStatistics.getClaimedCompensation()));
    } catch (Exception e) {
      log.error("计算已领取的DAO利息异常", e);
    }
    return total;
  }
}
