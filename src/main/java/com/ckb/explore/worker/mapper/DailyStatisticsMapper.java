package com.ckb.explore.worker.mapper;

import com.ckb.explore.worker.entity.DailyStatistics;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
* @author dell
* @description 针对表【daily_statistics】的数据库操作Mapper
* @createDate 2025-08-18 17:48:35
* @Entity com.ckb.explore.worker.entity.DailyStatistics
*/
@Mapper
public interface DailyStatisticsMapper extends BaseMapper<DailyStatistics> {
    @Select("select max(created_at_unixtimestamp) from daily_statistics")
    Long getMaxCreatAt();

    @Select("select min(created_at_unixtimestamp) from daily_statistics")
    Long getMinCreatAt();

    @Select("select * from daily_statistics where created_at_unixtimestamp = #{createAtUnixTimestamp}")
    DailyStatistics findDailyStatistic(@Param("createAtUnixTimestamp") Long createAtUnixTimestamp);

    @Select("select * from daily_statistics order by created_at_unixtimestamp desc limit 1")
    DailyStatistics getLastDayDailyStatistics();
}




