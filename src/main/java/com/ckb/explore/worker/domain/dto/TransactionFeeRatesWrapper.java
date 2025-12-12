package com.ckb.explore.worker.domain.dto;

import com.ckb.explore.worker.entity.TransactionFeeRates;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionFeeRatesWrapper {

  private List<TransactionFeeRates> data;
}
