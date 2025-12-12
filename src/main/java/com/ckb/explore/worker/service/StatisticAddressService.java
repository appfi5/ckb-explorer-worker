package com.ckb.explore.worker.service;

import com.ckb.explore.worker.entity.AddressBalanceRanking;
import com.ckb.explore.worker.entity.StatisticAddress;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
* @author dell
* @description 针对表【statistic_address】的数据库操作Service
* @createDate 2025-09-02 17:07:22
*/
public interface StatisticAddressService extends IService<StatisticAddress> {

  /**
   * 获取余额排名前50的地址列表
   * @return 地址余额排名列表
   */
  List<AddressBalanceRanking> getAddressBalanceRanking();
}
