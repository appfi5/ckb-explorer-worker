package com.ckb.explore.worker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ckb.explore.worker.entity.UdtDailyStatistics;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UdtDailyStatisticsMapper extends BaseMapper<UdtDailyStatistics> {
  @Select("select max(created_at_unixtimestamp) from udt_daily_statistics")
  Long getMaxCreatAt();

  @Select("select * from udt_daily_statistics where created_at_unixtimestamp = #{targetDate}")
  List<UdtDailyStatistics> selectByDate(@Param("targetDate") Long targetDate);
}
