package com.ckb.explore.worker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ckb.explore.worker.entity.OutputData;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface OutputDataMapper extends BaseMapper<OutputData> {

    @Select("<script>" +
            " select * from output_data where " +
            " output_id in  " +
            "      <foreach  item=\"item\" index=\"index\" collection=\"outputIds\" open=\"(\" separator=\",\" close=\")\">\n" +
            "        #{item}\n" +
            "      </foreach>" +
            "</script>")
    List<OutputData> selectByOutputIds(@Param("outputIds") List<Long> outputIds);


    @Select("select * from output_data where output_id = #{outputId} ")
    OutputData selectByOutputId(@Param("outputId") Long outputId);
}
