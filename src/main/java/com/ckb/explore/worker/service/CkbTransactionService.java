package com.ckb.explore.worker.service;

import com.ckb.explore.worker.entity.CkbTransaction;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author dell
* @description 针对表【ckb_transaction】的数据库操作Service
* @createDate 2025-08-22 18:30:38
*/
public interface CkbTransactionService extends IService<CkbTransaction> {

    List<CkbTransaction> selectByBlockNumberWithJoin(Long blockNumber);

    Long getTransactionsCountInPeriod( Long startedAt, Long endedAt);
}
