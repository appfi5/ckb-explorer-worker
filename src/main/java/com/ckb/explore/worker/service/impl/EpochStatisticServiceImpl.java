package com.ckb.explore.worker.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ckb.explore.worker.entity.EpochStatistic;
import com.ckb.explore.worker.mapper.BlockMapper;
import com.ckb.explore.worker.mapper.EpochStatisticMapper;
import com.ckb.explore.worker.service.EpochStatisticService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * EpochStatisticServiceImpl Epoch统计服务实现类
 * 实现Epoch统计相关的服务方法
 */
@Service
public class EpochStatisticServiceImpl extends ServiceImpl<EpochStatisticMapper, EpochStatistic> implements EpochStatisticService {

    @Resource
    private EpochStatisticMapper epochStatisticMapper;
    
    @Resource
    private BlockMapper blockMapper;

    @Override
    public Long getLatestEpochNumber() {
        // 从数据库中获取最新的Epoch编号
        return epochStatisticMapper.selectLatestEpochNumber();
    }

    @Override
    public EpochStatistic findByEpochNumber(Long epochNumber) {
        // 查找指定Epoch的统计记录
      return epochStatisticMapper.selectByEpochNumber(epochNumber);
    }
}