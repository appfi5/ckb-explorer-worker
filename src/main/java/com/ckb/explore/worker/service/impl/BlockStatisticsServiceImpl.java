package com.ckb.explore.worker.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ckb.explore.worker.entity.BlockStatistics;
import com.ckb.explore.worker.service.BlockStatisticsService;
import com.ckb.explore.worker.mapper.BlockStatisticsMapper;
import org.springframework.stereotype.Service;

/**
* @author dell
* @description 针对表【block_statistics】的数据库操作Service实现
* @createDate 2025-08-13 18:19:13
*/
@Service
public class BlockStatisticsServiceImpl extends ServiceImpl<BlockStatisticsMapper, BlockStatistics>
    implements BlockStatisticsService{

    @Override
    public Long getMaxBlockNumber(){
        Long maxBlockNumber = baseMapper.getMaxBlockNumber();
        return maxBlockNumber==null?0:maxBlockNumber;
    }


}




