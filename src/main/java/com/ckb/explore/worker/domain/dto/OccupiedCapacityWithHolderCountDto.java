package com.ckb.explore.worker.domain.dto;

import java.math.BigInteger;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OccupiedCapacityWithHolderCountDto {

  BigInteger occupiedCapacity;

  Long holderCount;
}
