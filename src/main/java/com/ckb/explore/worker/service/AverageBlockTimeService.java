package com.ckb.explore.worker.service;

import com.ckb.explore.worker.entity.RollingAvgBlockTime;
import java.util.List;

/**
 * AverageBlockTimeService 平均区块时间服务接口
 * 定义平均区块时间相关的服务方法
 */
public interface AverageBlockTimeService {

    /**
     * 刷新每小时平均区块时间物化视图
     */
    void refreshAverageBlockTimeByHour();
    
    /**
     * 刷新滚动平均区块时间物化视图
     */
    void refreshRollingAvgBlockTime();

    /**
     * 获取所有滚动平均区块时间数据
     * @return 滚动平均区块时间数据
     */
    List<RollingAvgBlockTime> getAllRollingAvgBlockTime();
}