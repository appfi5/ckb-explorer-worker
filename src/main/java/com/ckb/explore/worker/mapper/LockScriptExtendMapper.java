package com.ckb.explore.worker.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ckb.explore.worker.entity.DobCode;
import com.ckb.explore.worker.entity.LockScriptExtend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author dell
 * @description 针对表【lock_script_extend 】的数据库操作Mapper
 * @createDate 2025-10-16 13:30:00
 * @Entity com.ckb.explore.worker.entity.LockScriptExtend
 */
@DS("risingwave")
@Mapper
public interface LockScriptExtendMapper extends BaseMapper<LockScriptExtend> {

}




