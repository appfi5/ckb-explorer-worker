package com.ckb.explore.worker.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ckb.explore.worker.entity.DobExtend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @author dell
 * @description 针对表【dob_extend】的数据库操作Mapper
 * @createDate 2025-09-29 13:30:00
 * @Entity com.ckb.explore.worker.entity.DobExtend
 */
@DS("risingwave")
@Mapper
public interface DobExtendMapper extends BaseMapper<DobExtend> {


    @Select("select * from dob_extend where dob_code_hash = #{codeHash} and args =#{args}")
    DobExtend selectByCodeHashAndArgs( @Param("codeHash") byte[] codeHash, @Param("args") byte[] args);


    @Select("select max(id) from dob_extend ")
    Long getMaxId();

    @Select("select * from dob_extend where tags is null limit 100")
    List<DobExtend> getTagsNull();

    @Update("update  dob_extend set tags = string_to_array(#{tags}, ',')::varchar[] where id = #{id}")
    int updateTagsById( @Param("id") Long id, @Param("tags") String tags);


    @Select("select * from dob_extend where  args =#{args}")
    DobExtend selectByArgs( @Param("args") byte[] args);

    @Update("FLUSH")
    void flush();

}




