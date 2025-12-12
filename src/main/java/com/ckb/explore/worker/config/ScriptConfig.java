package com.ckb.explore.worker.config;


import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "script")
@Component
@Data
public class ScriptConfig {


  private List<LockScript> lockScripts;

  private List<TypeScript> typeScripts;


  @Data
  public static class LockScript {

    private String name;

    private String codeHash;

    private String hashType;

  }

  @Data
  public static class TypeScript {

    private String name;

    private String codeHash;

    private String hashType;

    private String args;

    private Integer cellType;

    private Integer version;
  }


  public LockScript getLockScriptByCodeHash(String codeHash) {
    return lockScripts.stream()
        .filter(lockScript -> Objects.equals(lockScript.getCodeHash(), codeHash)).findFirst().orElse(null);
  }

  public TypeScript getTypeScriptByCodeHash(String codeHash, String args) {
    List<TypeScript> typeScriptList=  typeScripts.stream()
        .filter(typeScript -> Objects.equals(typeScript.getCodeHash(), codeHash)).toList();
    //如果args不为空，则精准匹配
    if(!CollectionUtils.isEmpty(typeScriptList) && args !=null && !args.isEmpty()){
      return typeScriptList.stream().filter(typeScript -> Objects.equals(typeScript.getArgs(), args)).findFirst().orElse(null);
    }
    return typeScriptList.size() > 0 ? typeScriptList.get(0) : null;
  }

  public TypeScript getTypeScriptByCellType(Integer cellType, String codeHash) {
    List<TypeScript> typeScriptList=  typeScripts.stream()
            .filter(typeScript -> Objects.equals(typeScript.getCellType(), cellType)).toList();
    if(!CollectionUtils.isEmpty(typeScriptList) && codeHash != null && !codeHash.isEmpty() ){
      return typeScriptList.stream().filter(typeScript -> Objects.equals(typeScript.getCodeHash(), codeHash)).findFirst().orElse(null);
    }
    return typeScriptList.size() > 0 ? typeScriptList.get(0) : null;
  }

  public TypeScript getTypeScriptByCellType(Integer cellType, Integer version) {
    List<TypeScript> typeScriptList=  typeScripts.stream()
            .filter(typeScript -> Objects.equals(typeScript.getCellType(), cellType)).toList();
    if(!CollectionUtils.isEmpty(typeScriptList) && version!=null){
      return typeScriptList.stream().filter(typeScript -> Objects.equals(typeScript.getVersion(), version)).findFirst().orElse(null);
    }
    return typeScriptList.size() > 0 ? typeScriptList.get(0) : null;
  }



}
