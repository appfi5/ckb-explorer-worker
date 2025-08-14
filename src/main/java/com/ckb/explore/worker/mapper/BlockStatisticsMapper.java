package com.ckb.explore.worker.mapper;

import com.ckb.explore.worker.entity.BlockStatistics;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
* @author dell
* @description 针对表【block_statistics】的数据库操作Mapper
* @createDate 2025-08-13 18:19:13
* @Entity com.ckb.explore.worker.entity.BlockStatistics
*/
@Mapper
public interface BlockStatisticsMapper extends BaseMapper<BlockStatistics> {


    @Select("select max(block_number) from block_statistics")
    Long getMaxBlockNumber();

}




