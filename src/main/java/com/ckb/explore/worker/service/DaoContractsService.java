package com.ckb.explore.worker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ckb.explore.worker.entity.DaoContracts;

public interface DaoContractsService extends IService<DaoContracts> {

  void perform();
}
