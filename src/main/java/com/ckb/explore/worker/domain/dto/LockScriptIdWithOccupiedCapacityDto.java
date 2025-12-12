package com.ckb.explore.worker.domain.dto;

import java.math.BigInteger;
import lombok.Data;

@Data
public class LockScriptIdWithOccupiedCapacityDto {

  Long lockScriptId;

  BigInteger occupiedCapacity;
}
