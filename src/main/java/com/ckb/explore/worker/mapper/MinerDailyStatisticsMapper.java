package com.ckb.explore.worker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ckb.explore.worker.entity.MinerDailyStatistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MinerDailyStatisticsMapper extends BaseMapper<MinerDailyStatistics> {

  @Select("select max(created_at_unixtimestamp) from miner_daily_statistics")
  Long getMaxCreatAt();

  @Select("select * from miner_daily_statistics where created_at_unixtimestamp = #{date}")
  MinerDailyStatistics getByDate(Long  date);
}
