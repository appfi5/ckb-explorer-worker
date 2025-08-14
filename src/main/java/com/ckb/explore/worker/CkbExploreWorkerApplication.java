package com.ckb.explore.worker;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.ckb.explore.worker.mapper")
public class CkbExploreWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CkbExploreWorkerApplication.class, args);
    }

}
