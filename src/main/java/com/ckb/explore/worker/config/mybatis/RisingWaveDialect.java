package com.ckb.explore.worker.config.mybatis;
import com.baomidou.mybatisplus.extension.plugins.pagination.DialectModel;
import com.baomidou.mybatisplus.extension.plugins.pagination.dialects.IDialect;

/**
 * 自定义RisingWave分页方言，生成常量LIMIT/OFFSET的SQL
 */
public class RisingWaveDialect implements IDialect {


    @Override
    public DialectModel buildPaginationSql(String originalSql, long offset, long limit) {
        // 1. 校验参数（确保非负）
        long validOffset = Math.max(offset, 0);
        long validLimit = Math.max(limit, 0);

        // 2. 处理原始SQL：移除末尾的分号或空格
        String trimmedSql = originalSql.trim();
        if (trimmedSql.endsWith(";")) {
            trimmedSql = trimmedSql.substring(0, trimmedSql.length() - 1);
        }
       String sql = trimmedSql + " LIMIT " + validLimit + " OFFSET " + validOffset;
       return  new DialectModel(sql);
    }
}
