package com.ckb.explore.worker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ckb.explore.worker.entity.Block;
import com.ckb.explore.worker.entity.EpochStatistic;

/**
 * EpochStatisticService Epoch统计服务接口
 * 定义Epoch统计相关的服务方法
 */
public interface EpochStatisticService extends IService<EpochStatistic> {

    /**
     * 获取最新的Epoch编号
     * @return 最新的Epoch编号，如果没有则返回null
     */
    Long getLatestEpochNumber();
    
    /**
     * 根据Epoch编号查找或创建Epoch统计记录
     * @param epochNumber Epoch编号
     * @return Epoch统计记录
     */
    EpochStatistic findByEpochNumber(Long epochNumber);
}