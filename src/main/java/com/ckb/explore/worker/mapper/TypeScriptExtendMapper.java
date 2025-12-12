package com.ckb.explore.worker.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ckb.explore.worker.entity.TypeScriptExtend;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * TypeScriptExtendMapper 合约扩展接口
 * 用于操作type_script_extend表
 */
@Mapper
@DS("risingwave")
public interface TypeScriptExtendMapper extends BaseMapper<TypeScriptExtend> {

  @Select("<script>" +
      " select script_id from type_script_extend where \n" +
      " cell_type in  " +
      "      <foreach  item=\"item\" index=\"index\" collection=\"cellTypes\" open=\"(\" separator=\",\" close=\")\">\n" +
      "        #{item}\n" +
      "      </foreach>" +
      " order by script_id\n" +
      "</script>")
  Set<Long> getUdtIds(@Param("cellTypes") List<Integer> cellTypes);

}