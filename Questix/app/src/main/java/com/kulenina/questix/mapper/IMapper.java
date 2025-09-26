package com.kulenina.questix.mapper;

import java.util.Map;

public interface IMapper<T> {
    Map<String, Object> toMap(T object);

    T fromMap(Map<String, Object> map);

    T fromMap(Map<String, Object> map, String id);
}
