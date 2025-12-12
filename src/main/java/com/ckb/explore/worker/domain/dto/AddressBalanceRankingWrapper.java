package com.ckb.explore.worker.domain.dto;

import com.ckb.explore.worker.entity.AddressBalanceRanking;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressBalanceRankingWrapper {

  List<AddressBalanceRanking> data;
}
