package com.ckb.explore.worker.service;

import com.ckb.explore.worker.domain.dto.MarketDataDto;

public interface MarketDataService {

  MarketDataDto getMarketData(Long tipBlockNumber, String unit);
}
