package com.ckb.explore.worker.config.mybatis;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

@MappedJdbcTypes(JdbcType.ARRAY)
public class PgLongArrayTypeHandler extends BaseTypeHandler<List<Long>> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, List<Long> parameter,
      JdbcType jdbcType) throws SQLException {
    // 转换 List<Long> 为 Long[]
    Long[] array = parameter.toArray(new Long[0]);
    // 创建 PostgreSQL 数组（类型为 bigint[]）
    Array pgArray = ps.getConnection().createArrayOf("bigint", array);
    ps.setArray(i, pgArray);
  }


  @Override
  public List<Long> getNullableResult(ResultSet rs, String columnName) throws SQLException {

    return List.of();
  }

  @Override
  public List<Long> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return List.of();
  }

  @Override
  public List<Long> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return List.of();
  }
}
