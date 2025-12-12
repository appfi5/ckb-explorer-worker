package com.ckb.explore.worker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ckb.explore.worker.entity.StatisticInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * StatisticInfoMapper 统计信息Mapper接口
 * 用于操作statistic_infos表
 */
@Mapper
public interface StatisticInfoMapper extends BaseMapper<StatisticInfo> {

}