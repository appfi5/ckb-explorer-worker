package com.ckb.explore.worker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ckb.explore.worker.domain.dto.EpochTimeDistributionDto;
import com.ckb.explore.worker.entity.EpochStatistic;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * EpochStatisticMapper Epoch统计Mapper接口
 * 定义Epoch统计相关的数据库操作方法
 */
@Mapper
public interface EpochStatisticMapper extends BaseMapper<EpochStatistic> {

    /**
     * 获取最新的Epoch编号
     * @return 最新的Epoch编号，如果没有则返回null
     */
    @Select("SELECT MAX(epoch_number) FROM epoch_statistics")
    Long selectLatestEpochNumber();

    /**
     * 根据Epoch编号查找统计记录
     * @param epochNumber Epoch编号
     * @return Epoch统计记录，如果没有则返回null
     */
    @Select("SELECT * FROM epoch_statistics WHERE epoch_number = #{epochNumber}")
    EpochStatistic selectByEpochNumber(@Param("epochNumber") Long epochNumber);

    List<EpochTimeDistributionDto> getEpochTimeDistribution();
}