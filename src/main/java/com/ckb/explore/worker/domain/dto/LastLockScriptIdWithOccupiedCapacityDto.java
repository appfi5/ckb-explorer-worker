package com.ckb.explore.worker.domain.dto;

import java.math.BigInteger;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LastLockScriptIdWithOccupiedCapacityDto {

  Long date;

  Map<Long, BigInteger> lockScriptIdWithOccupiedCapacity;
}
