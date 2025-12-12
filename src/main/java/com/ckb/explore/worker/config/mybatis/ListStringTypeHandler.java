package com.ckb.explore.worker.config.mybatis;

import com.ckb.explore.worker.domain.dto.ListStringWrapper;
import com.ckb.explore.worker.utils.JsonUtil;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

@MappedTypes({ListStringWrapper.class})
public class ListStringTypeHandler extends
    BaseTypeHandler<ListStringWrapper> {

  @Override
  public void setNonNullParameter(java.sql.PreparedStatement ps, int i, ListStringWrapper parameter, JdbcType jdbcType) throws SQLException {
    String json = JsonUtil.toJSONString(parameter.getData() == null? new ArrayList(): parameter.getData());
    PGobject pgo = new PGobject();
    pgo.setType("jsonb");  // 显式声明为 jsonb
    pgo.setValue(json);

    ps.setObject(i, pgo); // 传入 PGobject
  }

  @Override
  public ListStringWrapper getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return parseJson(rs.getString(columnName));
  }

  @Override
  public ListStringWrapper getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return parseJson(rs.getString(columnIndex));
  }

  @Override
  public ListStringWrapper getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return parseJson(cs.getString(columnIndex));
  }

  private ListStringWrapper parseJson(String json) {
    if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
      return null;
    }
    ListStringWrapper listStringWrapper = new ListStringWrapper(JsonUtil.parseList(json, String[].class));
    return listStringWrapper;
  }
}
