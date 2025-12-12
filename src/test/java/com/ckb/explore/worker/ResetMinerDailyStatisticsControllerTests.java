package com.ckb.explore.worker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
public class ResetMinerDailyStatisticsControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void testManualTriggerWithoutStartDate() throws Exception {

    // 执行请求
    mockMvc.perform(post("/internal/miner_daily_statistics/manual_trigger")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().string("true"));

  }

  @Test
  public void testManualTriggerWithStartDate() throws Exception {

    // 执行请求
    mockMvc.perform(post("/internal/miner_daily_statistics/manual_trigger")
            .param("startDate", "2020-05-12")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().string("true"));
  }
}
