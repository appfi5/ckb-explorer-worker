package com.ckb.explore.worker.config.mybatis;

import com.ckb.explore.worker.utils.JsonUtil;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

@MappedTypes({Map.class})
public class MapTypeHandler extends
    BaseTypeHandler<Map> {

  @Override
  public void setNonNullParameter(java.sql.PreparedStatement ps, int i, Map parameter, JdbcType jdbcType) throws SQLException {
    String json = JsonUtil.toJSONString(parameter);
    PGobject pgo = new PGobject();
    pgo.setType("jsonb");  // 显式声明为 jsonb
    pgo.setValue(json);

    ps.setObject(i, pgo); // 传入 PGobject
  }

  @Override
  public Map getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return parseJson(rs.getString(columnName));
  }

  @Override
  public Map getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return parseJson(rs.getString(columnIndex));
  }

  @Override
  public Map getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return parseJson(cs.getString(columnIndex));
  }

  private Map parseJson(String json) {
    if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
      return null;
    }
    return JsonUtil.parseObject(json, Map.class);
  }
}
