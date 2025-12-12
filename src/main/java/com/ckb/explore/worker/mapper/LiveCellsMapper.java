package com.ckb.explore.worker.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.ckb.explore.worker.entity.LiveCells;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author dell
* @description 针对表【live_cells】的数据库操作Mapper
* @createDate 2025-09-02 13:43:49
* @Entity com.ckb.explore.worker.entity.LiveCells
*/
@DS("risingwave")
public interface LiveCellsMapper extends BaseMapper<LiveCells> {

}




