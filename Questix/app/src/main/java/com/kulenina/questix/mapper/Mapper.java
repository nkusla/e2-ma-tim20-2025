package com.kulenina.questix.mapper;

import com.kulenina.questix.model.IIdentifiable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Universal mapper that uses reflection to convert any object to/from Map<String, Object>
 * Works with any model class without requiring custom implementations
 */
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
            // Get all fields (including private ones)
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                // Make private fields accessible
                field.setAccessible(true);

                Object value = field.get(object);
                String fieldName = field.getName();

                // Skip the 'id' field as it's typically handled separately by Firestore
                // For IIdentifiable objects, we'll use the interface methods instead
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
            // Create new instance using default constructor
            T object = clazz.getDeclaredConstructor().newInstance();

            // Get all fields
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                String fieldName = field.getName();
                Object value = map.get(fieldName);

                // Make private fields accessible
                field.setAccessible(true);

                // Set the field value
                if (value != null) {
                    // Handle type conversion for common types
                    Object convertedValue = convertValue(value, field.getType());
                    field.set(object, convertedValue);
                }
            }

            // Set the ID field if provided
            if (id != null) {
                try {
                    Field idField = clazz.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(object, id);
                } catch (NoSuchFieldException e) {
                    // If the class implements IIdentifiable but doesn't have an 'id' field,
                    // this is a programming error
                    if (object instanceof IIdentifiable) {
                        throw new RuntimeException("Class " + clazz.getSimpleName() +
                            " implements IIdentifiable but doesn't have an 'id' field", e);
                    }
                    // For non-IIdentifiable objects, silently ignore missing id field
                }
            }

            return object;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert map to object", e);
        }
    }

    /**
     * Converts a value to the target field type
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // If types match, return as is
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // Handle common type conversions
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

        // If no conversion is possible, return the original value
        // This might cause a ClassCastException later, but that's better than silent failure
        return value;
    }
}
