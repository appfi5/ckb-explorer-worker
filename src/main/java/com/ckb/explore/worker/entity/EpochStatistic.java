package com.ckb.explore.worker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * EpochStatistic Epoch统计实体类
 * 对应数据库表：epoch_statistics
 */
@Data
@TableName("epoch_statistics")
public class EpochStatistic implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Epoch编号
     */
    private Long epochNumber;

    /**
     * 难度
     */
    private String difficulty;

    /**
     * Uncle率
     */
    private BigDecimal uncleRate;

    /**
     * 哈希率
     */
    private String hashRate;

    /**
     * Epoch时长
     */
    private Long epochTime;

    /**
     * Epoch长度（区块数）
     */
    private Integer epochLength;

    /**
     * 最大交易哈希
     */
    private byte[] largestTxHash;

    /**
     * 最大交易字节数
     */
    private Long largestTxBytes;

    /**
     * 最大交易cycles
     */
    private Long maxTxCycles;

    /**
     * 最大区块cycles
     */
    private Long maxBlockCycles;

    /**
     * 最大区块号
     */
    private Long largestBlockNumber;

    /**
     * 最大区块大小
     */
    private Long largestBlockSize;
}