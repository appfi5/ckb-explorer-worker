package com.ckb.explore.worker.service.impl;

import com.ckb.explore.worker.entity.RollingAvgBlockTime;
import com.ckb.explore.worker.mapper.AverageBlockTimeByHourMapper;
import com.ckb.explore.worker.mapper.RollingAvgBlockTimeMapper;
import com.ckb.explore.worker.service.AverageBlockTimeService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

/**
 * AverageBlockTimeServiceImpl 平均区块时间服务实现类 实现平均区块时间相关的服务方法
 */
@Service
@Slf4j
public class AverageBlockTimeServiceImpl implements AverageBlockTimeService {

  @Resource
  private RollingAvgBlockTimeMapper rollingAvgBlockTimeMapper;

  @Resource
  private AverageBlockTimeByHourMapper averageBlockTimeByHourMapper;

  @Override
  public void refreshAverageBlockTimeByHour() {
    averageBlockTimeByHourMapper.refreshMaterializedView();
  }

  @Override
  public void refreshRollingAvgBlockTime() {

    // 刷新滚动平均区块时间物化视图
    rollingAvgBlockTimeMapper.refreshMaterializedView();
  }

  @Override
  public List<RollingAvgBlockTime> getAllRollingAvgBlockTime() {
    return rollingAvgBlockTimeMapper.findAllOrderedByTimestamp();
  }
}