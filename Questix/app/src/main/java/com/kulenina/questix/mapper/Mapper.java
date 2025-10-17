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
            Class<?> currentClass = object.getClass();

            while (currentClass != null && currentClass != Object.class) {
                Field[] fields = currentClass.getDeclaredFields();

                for (Field field : fields) {
                    field.setAccessible(true);

                    Object value = field.get(object);
                    String fieldName = field.getName();

                    // Only add if not already present (child class fields take precedence)
                    // and if it's not the id field
                    if (!"id".equals(fieldName) && !map.containsKey(fieldName)) {
                        map.put(fieldName, value);
                    }
                }

                currentClass = currentClass.getSuperclass();
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

            Class<?> currentClass = clazz;
            while (currentClass != null && currentClass != Object.class) {
                Field[] fields = currentClass.getDeclaredFields();

                for (Field field : fields) {
                    String fieldName = field.getName();
                    Object value = map.get(fieldName);

                    field.setAccessible(true);

                    if (value != null) {
                        Object convertedValue = convertValue(value, field.getType());
                        field.set(object, convertedValue);
                    }
                }

                currentClass = currentClass.getSuperclass();
            }

            if (id != null) {
                Class<?> searchClass = clazz;
                boolean idSet = false;

                while (searchClass != null && searchClass != Object.class && !idSet) {
                    try {
                        Field idField = searchClass.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(object, id);
                        idSet = true;
                    } catch (NoSuchFieldException e) {
                        searchClass = searchClass.getSuperclass();
                    }
                }

                if (!idSet && object instanceof IIdentifiable) {
                    throw new RuntimeException("Class " + clazz.getSimpleName() +
                        " implements IIdentifiable but doesn't have an 'id' field in its hierarchy");
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

        if (targetType.isEnum()) {
            if (value instanceof String) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends Enum> enumType = (Class<? extends Enum>) targetType;
                    return Enum.valueOf(enumType, (String) value);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
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
