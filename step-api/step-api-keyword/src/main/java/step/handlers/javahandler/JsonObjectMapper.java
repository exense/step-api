package step.handlers.javahandler;

import javax.json.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class JsonObjectMapper {

    public static Object jsonValueToJavaObject(JsonValue jsonValue, Type type) {
        Class<?> valueClass = getTypeClass(type);

        Object value;
        if (jsonValue == null || jsonValue.equals(JsonValue.NULL)) {
            value = null;
        } else {
            if (jsonValue instanceof JsonArray) {
                value = jsonArrayToObject((JsonArray) jsonValue, type);
            } else if (jsonValue instanceof JsonObject) {
                if(Map.class.isAssignableFrom(valueClass)) {
                    Map map = jsonObjectToMap((JsonObject) jsonValue, (ParameterizedType) type, valueClass);
                    value = map;
                } else {
                    value = jsonObjectToObject((JsonObject) jsonValue, type);
                }
            } else if (jsonValue instanceof JsonString) {
                if (String.class.isAssignableFrom(valueClass)) {
                    value = ((JsonString) jsonValue).getString();
                } else {
                    throw notMappableValueClass(jsonValue, valueClass);
                }
            } else if (jsonValue instanceof JsonNumber) {
                JsonNumber jsonNumber = (JsonNumber) jsonValue;
                if (Integer.class.isAssignableFrom(valueClass) || valueClass.equals(int.class)) {
                    value = (int) jsonNumber.longValue();
                } else if (Double.class.isAssignableFrom(valueClass) || valueClass.equals(double.class)) {
                    value = jsonNumber.doubleValue();
                } else if (Long.class.isAssignableFrom(valueClass) || valueClass.equals(long.class)) {
                    value = jsonNumber.longValue();
                } else if (BigDecimal.class.isAssignableFrom(valueClass)) {
                    value = jsonNumber.bigDecimalValue();
                } else if (BigInteger.class.isAssignableFrom(valueClass)) {
                    value = jsonNumber.bigIntegerValue();
                } else {
                    throw notMappableValueClass(jsonValue, valueClass);
                }
            } else if (jsonValue.equals(JsonValue.TRUE) || jsonValue.equals(JsonValue.FALSE)) {
                if (Boolean.class.isAssignableFrom(valueClass) || valueClass.equals(boolean.class)) {
                    value = jsonValue.equals(JsonValue.TRUE);
                } else {
                    throw notMappableValueClass(jsonValue, valueClass);
                }
            } else {
                throw new RuntimeException("Unsupported JSON value type" + jsonValue.getValueType());
            }
        }
        return value;
    }

    private static Map jsonObjectToMap(JsonObject jsonValue, ParameterizedType type, Class<?> valueClass) {
        Map map;
        //If it is declared with a Map interface, default to HashMap
        if (valueClass.isInterface()) {
            map = new HashMap<>();
        } else {
            try {
                map = (Map) valueClass.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        Type actualTypeArgument = type.getActualTypeArguments()[1];
        jsonValue.forEach((k, v) -> {
            map.put(k, jsonValueToJavaObject(v, actualTypeArgument));
        });
        return map;
    }

    public static JsonObject javaObjectToJsonObject(Object value) {
        if(value != null) {
            return valueToJsonObject(value, value.getClass()).build();
        } else {
            return null;
        }
    };

    private static RuntimeException notMappableValueClass(JsonValue jsonValue, Class<?> valueClass) {
        return new RuntimeException("Unable to map JSON value of type " + jsonValue.getValueType() + " to java type" + valueClass);
    }

    public static Class<?> getTypeClass(Type type) {
        Class<?> valueClass;
        if (type instanceof Class) {
            valueClass = (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            valueClass = (Class<?>) ((ParameterizedType) type).getRawType();
        } else {
            throw unsupportedType(type);
        }
        return valueClass;
    }

    private static Object jsonArrayToObject(JsonArray jsonValue, Type type) {
        Class<?> valueClass = getTypeClass(type);
        Object value;
        Type arrayValueType;
        if(valueClass.isArray()) {
            arrayValueType = valueClass.getComponentType();
        } else {
            arrayValueType = resolveGenericTypeForArrayOrCollection(type);
        }
        List<Object> list = jsonValue.stream().map(e -> jsonValueToJavaObject(e, arrayValueType)).collect(Collectors.toList());
        if (valueClass.isArray()) {
            value = toArray(arrayValueType, list);
        } else {
            value = list;
        }
        return value;
    }

    public static Object[] toArray(Type arrayValueType, List<Object> list) {
        return list.toArray((Object[]) Array.newInstance(getTypeClass(arrayValueType), list.size()));
    }

    public static Type resolveGenericTypeForArrayOrCollection(Type type) {
        Class<?> typeClass = getTypeClass(type);
        if(typeClass.isArray()) {
            return typeClass.getComponentType();
        } else {
            if (!(type instanceof ParameterizedType)) {
                throw unsupportedType(type);
            }

            Type[] collectionGenerics = ((ParameterizedType) type).getActualTypeArguments();
            if (collectionGenerics.length != 1) {
                throw unsupportedType(type);
            }

            return collectionGenerics[0];
        }
    }

    private static IllegalArgumentException unsupportedType(Type type) {
        return new IllegalArgumentException("Unsupported type " + type);
    }

    private static <T extends Object> T jsonObjectToObject(JsonObject nestedObjectFromInput, Type type) {
        Class<?> typeClass = getTypeClass(type);
        Object value;
        try {
            value = typeClass.getConstructor().newInstance();
            List<Field> fields = getAllFields(value.getClass());
            for (Field field : fields) {
                writeField(field, value, jsonValueToJavaObject(nestedObjectFromInput.getOrDefault(field.getName(), null), field.getGenericType()));
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        return (T) value;
    }

    public static List<Field> getAllFields(final Class<?> cls) {
        final List<Field> allFields = new ArrayList<>();
        getAllFieldsRecursive(allFields, cls);
        return allFields;
    }

    private static void getAllFieldsRecursive(List<Field> allFields, Class<?> currentClass) {
        Class<?> superclass = currentClass.getSuperclass();
        if (superclass != null) {
            getAllFieldsRecursive(allFields, currentClass.getSuperclass());
        }
        Arrays.stream(currentClass.getDeclaredFields()).filter(f -> !f.isSynthetic()).forEach(allFields::add);
    }

    private static void writeField(final Field field, final Object target, final Object value) throws IllegalAccessException {
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        field.set(target, value);
    }

    protected static void addValueToJsonObject(JsonObjectBuilder objectBuilder, String inputName, Object value) {
        if (value == null) {
            objectBuilder.addNull(inputName);
        } else {
            Class<?> valueType = value.getClass();
            if (valueType.equals(String.class)) {
                objectBuilder.add(inputName, (String) value);
            } else if (valueType.equals(Integer.class) || valueType.equals(int.class)) {
                objectBuilder.add(inputName, (int) value);
            } else if (valueType.equals(Long.class) || valueType.equals(long.class)) {
                objectBuilder.add(inputName, (long) value);
            } else if (valueType.equals(Boolean.class) || valueType.equals(boolean.class)) {
                objectBuilder.add(inputName, (boolean) value);
            } else if (valueType.equals(Double.class) || valueType.equals(double.class)) {
                objectBuilder.add(inputName, (double) value);
            } else if (valueType.equals(BigDecimal.class)) {
                objectBuilder.add(inputName, (BigDecimal) value);
            } else if (valueType.equals(BigInteger.class)) {
                objectBuilder.add(inputName, (BigInteger) value);
            } else if (Collection.class.isAssignableFrom(valueType)) {
                Collection collection = (Collection) value;
                objectBuilder.add(inputName, collectionToJsonArray(collection));
            } else if (valueType.isArray()) {
                objectBuilder.add(inputName, collectionToJsonArray(Arrays.asList((Object[]) value)));
            } else {
                objectBuilder.add(inputName, valueToJsonObject(value, valueType));
            }
        }
    }

    private static JsonArrayBuilder collectionToJsonArray(Collection collection) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        collection.forEach(e -> addValueToJsonArray(arrayBuilder, e));
        return arrayBuilder;
    }

    private static JsonObjectBuilder valueToJsonObject(Object value, Class<?> valueType) {
        JsonObjectBuilder objectBuilder2 = Json.createObjectBuilder();
        List<Field> fields = getAllFields(valueType);
        fields.forEach(field -> {
            try {
                addValueToJsonObject(objectBuilder2, field.getName(), field.get(value));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        return objectBuilder2;
    }

    private static void addValueToJsonArray(JsonArrayBuilder arrayBuilder, Object value) {
        if (value == null) {
            arrayBuilder.addNull();
        } else {
            Class<?> valueType = value.getClass();
            if (valueType.equals(String.class)) {
                arrayBuilder.add((String) value);
            } else if (valueType.equals(Integer.class) || valueType.equals(int.class)) {
                arrayBuilder.add((int) value);
            } else if (valueType.equals(Long.class) || valueType.equals(long.class)) {
                arrayBuilder.add((long) value);
            } else if (valueType.equals(Boolean.class) || valueType.equals(boolean.class)) {
                arrayBuilder.add((boolean) value);
            } else if (valueType.equals(Double.class) || valueType.equals(double.class)) {
                arrayBuilder.add((double) value);
            } else if (valueType.equals(BigDecimal.class)) {
                arrayBuilder.add((BigDecimal) value);
            } else if (valueType.equals(BigInteger.class)) {
                arrayBuilder.add((BigInteger) value);
            } else if (Collection.class.isAssignableFrom(valueType)) {
                Collection collection = (Collection) value;
                arrayBuilder.add(collectionToJsonArray(collection));
            } else {
                arrayBuilder.add(valueToJsonObject(value, valueType));
            }
        }
    }
}
