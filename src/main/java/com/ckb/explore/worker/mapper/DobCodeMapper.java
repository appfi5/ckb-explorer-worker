package com.ckb.explore.worker.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ckb.explore.worker.entity.DobCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author dell
 * @description 针对表【dob_code】的数据库操作Mapper
 * @createDate 2025-09-29 13:30:00
 * @Entity com.ckb.explore.worker.entity.DobExtend
 */
@DS("risingwave")
@Mapper
public interface DobCodeMapper extends BaseMapper<DobCode> {

    @Select("select max(dob_code_script_id) from dob_code ")
    Long getMaxScriptId();
}




