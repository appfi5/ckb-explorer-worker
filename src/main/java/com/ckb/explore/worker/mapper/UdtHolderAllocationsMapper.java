package com.ckb.explore.worker.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.ckb.explore.worker.entity.UdtHolderAllocations;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
* @author dell
* @description 针对表【udt_holder_allocations】的数据库操作Mapper
* @createDate 2025-08-27 14:46:54
* @Entity com.ckb.explore.worker.entity.UdtHolderAllocations
*/
@DS("risingwave")
public interface UdtHolderAllocationsMapper extends BaseMapper<UdtHolderAllocations> {



}




