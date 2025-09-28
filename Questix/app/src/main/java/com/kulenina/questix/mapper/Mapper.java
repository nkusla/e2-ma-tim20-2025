package com.kulenina.questix.mapper;

import com.kulenina.questix.model.IIdentifiable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class Mapper<T> implements IMapper<T> {
    private final Class<T> clazz;

    public Mapper(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Map<String, Object> toMap(T object) {
        if (object == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();

        try {

            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {

                field.setAccessible(true);

                Object value = field.get(object);
                String fieldName = field.getName();

                if (!"id".equals(fieldName)) {
                    map.put(fieldName, value);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to convert object to map", e);
        }

        return map;
    }

    @Override
    public T fromMap(Map<String, Object> map) {
        return fromMap(map, null);
    }

    @Override
    public T fromMap(Map<String, Object> map, String id) {
        if (map == null) {
            return null;
        }

        try {
            T object = clazz.getDeclaredConstructor().newInstance();

            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                String fieldName = field.getName();
                Object value = map.get(fieldName);


                field.setAccessible(true);


                if (value != null) {
                    Object convertedValue = convertValue(value, field.getType());
                    field.set(object, convertedValue);
                }
            }

            if (id != null) {
                try {
                    Field idField = clazz.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(object, id);
                } catch (NoSuchFieldException e) {
                    if (object instanceof IIdentifiable) {
                        throw new RuntimeException("Class " + clazz.getSimpleName() +
                            " implements IIdentifiable but doesn't have an 'id' field", e);
                    }
                }
            }

            return object;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert map to object", e);
        }
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
        } else if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value instanceof String) {
                return Long.parseLong((String) value);
            }
        } else if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
        } else if (targetType == Float.class || targetType == float.class) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            } else if (value instanceof String) {
                return Float.parseFloat((String) value);
            }
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) {
                return value;
            } else if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
        }

        return value;
    }
}
