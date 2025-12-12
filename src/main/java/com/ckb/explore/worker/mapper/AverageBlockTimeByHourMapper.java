package com.ckb.explore.worker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ckb.explore.worker.entity.AverageBlockTimeByHour;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AverageBlockTimeByHourMapper extends BaseMapper<AverageBlockTimeByHour> {

  /**
   * 1. 按 timestamp 升序查询所有数据（复现 Rails 的 default_scope）
   * @return 排序后的滚动平均区块时间列表
   */
  @Select("SELECT timestamp, avg_block_time_per_hour FROM average_block_time_by_hour ORDER BY timestamp ASC")
  List<AverageBlockTimeByHour> findAllOrderedByTimestamp();

  /**
   * 2. 并发刷新物化视图（复现 Rails 的 refresh 方法）
   * @return 影响行数（PostgreSQL 执行成功返回 0，无需关注）
   */
  @Update("refresh materialized view CONCURRENTLY average_block_time_by_hour")
  int refreshMaterializedView();
}
