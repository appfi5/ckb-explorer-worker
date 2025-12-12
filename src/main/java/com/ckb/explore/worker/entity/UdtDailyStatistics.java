package com.ckb.explore.worker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class UdtDailyStatistics implements Serializable {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long scriptId;

  private Integer ckbTransactionsCount;

  private Integer holdersCount;

  private Long createdAtUnixtimestamp;

  private OffsetDateTime createdAt;

  private OffsetDateTime updatedAt;

  // 版本号：@Version标记为乐观锁字段，更新时自动+1；新增时默认0
  @Version
  private Integer version;

  private static final long serialVersionUID = 1L;
}
