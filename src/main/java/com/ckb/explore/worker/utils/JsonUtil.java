package com.ckb.explore.worker.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtil {
  private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static String datePattern = "yyyy-MM-dd";
  private static String dateTimePattern = "yyyy-MM-dd HH:mm:ss";

  public static String toJSONString(Object obj) {
    if (obj == null) {
      return "";
    } else {
      try {
        return objectMapper.writeValueAsString(obj);
      } catch (JsonProcessingException e) {
        log.error("json object parse error.", e);
        throw new RuntimeException("json object parse to string occurred exception.");
      }
    }
  }

  public static <T> T parseObject(String json, Class<T> clazz) {
    if (json != null && json.length() != 0) {
      try {
        return (T)objectMapper.readValue(json, clazz);
      } catch (IOException e) {
        log.error("json string parse error.", e);
        throw new RuntimeException("json string parse to object occurred exception.");
      }
    } else {
      return null;
    }
  }

  public static <T> T parseObject(String json, TypeReference<T> typeReference) {
    if (json != null && json.length() != 0) {
      try {
        return (T)objectMapper.readValue(json, typeReference);
      } catch (IOException e) {
        log.error("json string parse error.", e);
        throw new RuntimeException("json string parse to object occurred exception.");
      }
    } else {
      return null;
    }
  }

  public static <T> List<T> parseList(String json, Class<T> clazz) {
    if (json != null && json.length() != 0) {
      try {
        CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
        return (List)objectMapper.readValue(json, collectionType);
      } catch (IOException e) {
        log.error("json string parse error.", e);
        throw new RuntimeException("json string parse to object occurred exception.");
      }
    } else {
      return new ArrayList();
    }
  }
}
