package com.ckb.explore.worker.service;

import com.ckb.explore.worker.entity.Script;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
* @author dell
* @description 针对表【script】的数据库操作Service
* @createDate 2025-08-22 18:30:00
*/
public interface ScriptService extends IService<Script> {
  List<byte[]>  getZeroLockScripts();
}
