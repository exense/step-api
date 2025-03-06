package step.handlers.javahandler;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static step.handlers.javahandler.JsonObjectMapper.*;

public class SimplifiedObjectDeserializer {

    public static Object parse(String string, Type type) {
        Class<?> typeClass = getTypeClass(type);
        if(typeClass == String.class) {
            return string;
        } else if (typeClass == Integer.class || typeClass == int.class) {
            return Integer.parseInt(string);
        } else if (typeClass == Long.class || typeClass == long.class) {
            return Long.parseLong(string);
        }  else if (typeClass == Double.class || typeClass == double.class) {
            return Double.parseDouble(string);
        } else if (typeClass == BigDecimal.class) {
            return new BigDecimal(string);
        } else if (typeClass == BigInteger.class) {
            return new BigInteger(string);
        } else if (typeClass == Boolean.class || typeClass == boolean.class) {
            return Boolean.parseBoolean(string);
        } else if (List.class.isAssignableFrom(typeClass)) {
            return parseArrayAsList(string, type);
        } else if (typeClass.isArray()) {
            return parseArrayAsArray(string, typeClass);
        } else if (Map.class.isAssignableFrom(typeClass)) {
            JsonObject jsonObject = Json.createReader(new StringReader(string)).readObject();
            return JsonObjectMapper.jsonValueToJavaObject(jsonObject, type);
        } else {
            throw new RuntimeException("Type " + typeClass.getName() + " not supported");
        }
    }

    private static List<Object> parseArrayAsList(String string, Type type) {
        Type genericType = resolveGenericTypeForArrayOrCollection(type);
        return parseArrayString(string, genericType);
    }

    private static Object[] parseArrayAsArray(String string, Class<?> typeClass) {
        Class<?> componentType = typeClass.getComponentType();
        return toArray(typeClass.getComponentType(), parseArrayString(string, componentType));
    }

    private static List<Object> parseArrayString(String string, Type genericType) {
        String[] split = string.split(";");
        return Arrays.stream(split).map(e -> parse(e, genericType)).collect(Collectors.toList());
    }
}
