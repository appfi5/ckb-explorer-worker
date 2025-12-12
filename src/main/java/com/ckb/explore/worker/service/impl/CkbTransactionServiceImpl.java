package com.ckb.explore.worker.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ckb.explore.worker.entity.CkbTransaction;
import com.ckb.explore.worker.service.CkbTransactionService;
import com.ckb.explore.worker.mapper.CkbTransactionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author dell
* @description 针对表【ckb_transaction】的数据库操作Service实现
* @createDate 2025-08-22 18:30:37
*/
@Service
public class CkbTransactionServiceImpl extends ServiceImpl<CkbTransactionMapper, CkbTransaction>
    implements CkbTransactionService{

    @Resource
    CkbTransactionMapper ckbTransactionMapper;

    @Override
    public List<CkbTransaction> selectByBlockNumberWithJoin(Long blockNumber){
        return ckbTransactionMapper.selectByBlockNumberWithJoin(blockNumber);
    }

  @Override
  public Long getTransactionsCountInPeriod(Long startedAt, Long endedAt) {
      return ckbTransactionMapper.getTransactionsCountInPeriod(startedAt, endedAt);
  }
}




