package com.ckb.explore.worker;

import com.ckb.explore.worker.task.UdtDailyStatisticTask;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class UdtDailyStatisticTests {

  @Resource
  UdtDailyStatisticTask udtDailyStatisticTask;

  @Test
  public void test() throws InterruptedException {
    LocalDate startDate = LocalDate.of(2022, 2, 11);
    udtDailyStatisticTask.perform(startDate);
  }
}
