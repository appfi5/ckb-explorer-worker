package com.ckb.explore.worker.service;

import com.ckb.explore.worker.entity.BlockStatistics;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author dell
* @description 针对表【block_statistics】的数据库操作Service
* @createDate 2025-08-13 18:19:13
*/
public interface BlockStatisticsService extends IService<BlockStatistics> {

    Long getMaxBlockNumber();
}
