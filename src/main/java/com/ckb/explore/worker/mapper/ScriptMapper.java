package com.ckb.explore.worker.mapper;

import com.ckb.explore.worker.entity.Script;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author dell
* @description 针对表【script】的数据库操作Mapper
* @createDate 2025-08-22 18:30:00
* @Entity com.ckb.explore.worker.entity.Script
*/
public interface ScriptMapper extends BaseMapper<Script> {

  @Select("select count(*) from script where timestamp < #{endAt} and is_typescript = 0")
  Long getAddressesCount(@Param("endAt") Long endAt);

  @Select("select count(*) from script where timestamp >= #{startAt} and timestamp < #{endAt} and is_typescript = 0")
  Long getAddressesCountByTimeRange(@Param("startAt") Long startAt, @Param("endAt") Long endAt);


  @Select("select * from script where id > #{lastId} order by id asc  limit #{limit}")
  List<Script> getBatchById( @Param("lastId") long lastId, @Param("limit") int limit);

  @Select("<script>" +
          "select * from script where id > #{lastId} and code_hash in \n" +
          "<foreach  item=\"item\" index=\"index\" collection=\"codeHashes\" open=\"(\" separator=\",\" close=\")\">\n" +
          "  #{item}\n" +
          "</foreach>\n" +
          "order by id asc  limit #{limit} \n" +
          "</script>")
  List<Script> getBatchCodeHashesAndId(@Param("lastId") long lastId, @Param("codeHashes") List<byte[]> codeHashes, @Param("limit") int limit);

  @Select("SELECT id from script where code_hash = #{code_hash} and is_typescript = 0")
  List<Long> getZeroLockScriptId(@Param("code_hash") byte[] code_hash);

  @Select("SELECT * from script where code_hash = #{code_hash} and is_typescript = 0")
  List<Script> getZeroLockScripts(@Param("code_hash") byte[] code_hash);

  @Select("SELECT * from script where script_hash = #{script_hash} ")
  Script findByScriptHashScript(@Param("script_hash") byte[] script_hash);
}




