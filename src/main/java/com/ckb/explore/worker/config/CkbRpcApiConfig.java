package com.ckb.explore.worker.config;

import org.nervos.ckb.CkbRpcApi;
import org.nervos.ckb.service.Api;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CkbRpcApiConfig {

    @Value("${ckb.nodeUrl:http://localhost:8114}")
    private String nodeUrl;

    @Bean
    public CkbRpcApi ckbApi() {
        return new Api(nodeUrl);
    }
}