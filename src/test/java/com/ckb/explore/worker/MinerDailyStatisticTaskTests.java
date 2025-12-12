package com.ckb.explore.worker;

import com.ckb.explore.worker.task.MinerDailyStatisticTask;
import com.ckb.explore.worker.utils.TypeConversionUtil;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.nervos.ckb.utils.Numeric;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class MinerDailyStatisticTaskTests {

  @Resource
  private MinerDailyStatisticTask minerDailyStatisticTask;

  @Test
  public void test() throws InterruptedException {
    minerDailyStatisticTask.scheduleMinerDailyStatistic();
  }

  @Test
  public void testMinerHash() {
    var codeHash = Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000");
    var args = Numeric.hexStringToByteArray("0x");
    var scriptHash = TypeConversionUtil.lockScriptToScriptHash(codeHash, args, (short) 1);
    log.info(Numeric.toHexString(scriptHash));
  }


}
