package com.ckb.explore.worker.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.ckb.explore.worker.config.mybatis.RisingWaveDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置类
 * 用于配置分页插件等功能
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 添加分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件，不指定数据库类型，让MyBatis Plus自动检测
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        //rising wave 分页设置
        paginationInnerInterceptor.setDialect(new RisingWaveDialect());
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        // 添加乐观锁插件，用于version字段自增
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

}