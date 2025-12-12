package com.ckb.explore.worker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * StatisticAddress 地址统计信息实体类
 * 对应数据库表：statistic_address
 */
@Data
@TableName("statistic_address")
public class StatisticAddress {

    /**
     * 锁脚本ID
     */
    private Long lockScriptId;

    /**
     * 地址余额
     */
    private BigInteger balance;

    /**
     * 存活Cell数量
     */
    private Long liveCellsCount;

    /**
     * 占用的余额
     */
    private BigInteger balanceOccupied;
}
