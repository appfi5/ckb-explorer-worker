package com.ckb.explore.worker.config.mybatis;

import com.ckb.explore.worker.domain.dto.AddressBalanceRankingWrapper;
import com.ckb.explore.worker.entity.AddressBalanceRanking;
import com.ckb.explore.worker.utils.JsonUtil;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

@Slf4j
@MappedTypes({AddressBalanceRankingWrapper.class})
public class AddressBalanceRankingTypeHandler extends BaseTypeHandler<AddressBalanceRankingWrapper> {

  @Override
  public void setNonNullParameter(java.sql.PreparedStatement ps, int i, AddressBalanceRankingWrapper parameter, JdbcType jdbcType) throws SQLException {
    String json = JsonUtil.toJSONString(parameter.getData() == null? new ArrayList(): parameter.getData());
    PGobject pgo = new PGobject();
    pgo.setType("jsonb");  // 显式声明为 jsonb
    pgo.setValue(json);

    ps.setObject(i, pgo); // 传入 PGobject
  }

  @Override
  public AddressBalanceRankingWrapper getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return parseJson(rs.getString(columnName));
  }

  @Override
  public AddressBalanceRankingWrapper getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return parseJson(rs.getString(columnIndex));
  }

  @Override
  public AddressBalanceRankingWrapper getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return parseJson(cs.getString(columnIndex));
  }

  private AddressBalanceRankingWrapper parseJson(String json) {
    if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
      return new AddressBalanceRankingWrapper(null);
    }
    try {
      var data = JsonUtil.parseList(json, AddressBalanceRanking.class);
      return new AddressBalanceRankingWrapper(data);
    } catch (Exception e) {
      // 关键：报错时必须携带JSON内容，否则无法排查！
      log.error("解析JSON失败，JSON内容: {}", json, e);
      throw new RuntimeException("解析AddressBalanceRanking列表失败，JSON: " + json, e);
    }
  }
}
