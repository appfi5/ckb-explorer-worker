package com.ckb.explore.worker.config.mybatis;

import com.ckb.explore.worker.entity.TransactionFeeRates;
import com.ckb.explore.worker.domain.dto.TransactionFeeRatesWrapper;
import com.ckb.explore.worker.utils.JsonUtil;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

@MappedTypes({TransactionFeeRatesWrapper.class})
public class TransactionFeeRatesTypeHandler  extends BaseTypeHandler<TransactionFeeRatesWrapper> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, TransactionFeeRatesWrapper parameter,
      JdbcType jdbcType) throws SQLException {
    String json = JsonUtil.toJSONString(parameter.getData() == null? new ArrayList(): parameter.getData());
    PGobject pgo = new PGobject();
    pgo.setType("jsonb");  // 显式声明为 jsonb
    pgo.setValue(json);

    ps.setObject(i, pgo); // 传入 PGobject
  }

  @Override
  public TransactionFeeRatesWrapper getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return parseJson(rs.getString(columnName));
  }

  @Override
  public TransactionFeeRatesWrapper getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return parseJson(rs.getString(columnIndex));
  }

  @Override
  public TransactionFeeRatesWrapper getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return parseJson(cs.getString(columnIndex));
  }

  private TransactionFeeRatesWrapper parseJson(String json) {
    if (json == null || json.trim().isEmpty() || "null".equals(json.trim())|| "{}".equals(json.trim())) {
      return null;
    }
    TransactionFeeRatesWrapper transactionFeeRatesWrapper = new TransactionFeeRatesWrapper(JsonUtil.parseList(json, TransactionFeeRates.class));
    return transactionFeeRatesWrapper;
  }
}
