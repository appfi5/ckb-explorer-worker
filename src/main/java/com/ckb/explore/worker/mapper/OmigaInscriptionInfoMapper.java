package com.ckb.explore.worker.mapper;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ckb.explore.worker.entity.OmigaInscriptionInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@DS("risingwave")
@Mapper
/**
 * @author dell
 * @description 针对表【omiga_inscription_info】的数据库操作Mapper
 * @createDate 2025-12-22 14:47:00
 * @Entity com.ckb.explore.worker.entity.DobExtend
 */
public interface OmigaInscriptionInfoMapper extends BaseMapper<OmigaInscriptionInfo> {

    @Select("SELECT * from omiga_inscription_info where udt_hash = #{udtHash} limit 1")
    OmigaInscriptionInfo findByUdtHash(@Param("udtHash") byte[] udtHash);
}
