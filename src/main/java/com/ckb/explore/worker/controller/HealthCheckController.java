package com.ckb.explore.worker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/health_check")
@Slf4j
public class HealthCheckController {

  @GetMapping
  public Boolean healthCheck() {
    return true;
  }
}
