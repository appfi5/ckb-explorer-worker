package com.ckb.explore.worker.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ckb.explore.worker.domain.dto.TransactionConfirmationTimeDto;
import com.ckb.explore.worker.entity.CkbPendingTransaction;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
@DS("pending")
public interface CkbPendingTransactionMapper extends BaseMapper<CkbPendingTransaction> {
  @Select("select '0x' || encode(tx_hash, 'hex') as tx_hash, (updated_at - created_at) / 1000 as confirmation_time from ckb_transaction where status = 1 order by updated_at desc LIMIT 10000")
  List<TransactionConfirmationTimeDto> selectTransactionConfirmationTimes();
}