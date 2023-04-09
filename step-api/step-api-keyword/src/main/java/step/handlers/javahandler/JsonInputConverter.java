package step.handlers.javahandler;

import jakarta.json.JsonObjectBuilder;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.json.JsonObject;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonInputConverter {

	public static void addValueToJsonBuilder(String defaultValue, JsonObjectBuilder builder, Class<?> type, String jsonName) throws IllegalArgumentException {
		if(String.class.isAssignableFrom(type)){
			builder.add(jsonName, defaultValue);
		} else if(Boolean.class.isAssignableFrom(type)){
			builder.add(jsonName, Boolean.parseBoolean(defaultValue));
		} else if(Integer.class.isAssignableFrom(type)){
			builder.add(jsonName, Integer.parseInt(defaultValue));
		} else if(Long.class.isAssignableFrom(type)){
			builder.add(jsonName, Long.parseLong(defaultValue));
		} else if(Double.class.isAssignableFrom(type)){
			builder.add(jsonName, Double.parseDouble(defaultValue));
		} else if(BigInteger.class.isAssignableFrom(type)){
			builder.add(jsonName, BigInteger.valueOf(Long.parseLong(defaultValue)));
		} else if(BigDecimal.class.isAssignableFrom(type)){
			builder.add(jsonName, BigDecimal.valueOf(Double.parseDouble(defaultValue)));
		} else {
			throw new IllegalArgumentException("Unable to apply the following type to json builder: " + type);
		}
	}

	public static String resolveJsonPropertyType(Class<?> type) {
		if (String.class.isAssignableFrom(type)) {
			return "string";
		} else if (Boolean.class.isAssignableFrom(type)) {
			return "boolean";
		} else if (Number.class.isAssignableFrom(type)) {
			return "number";
		} else {
			return "object";
		}

		// TODO: support arrays?
	}

	public static Object getValueFromJsonInput(JsonObject input, String name, Class<?> valueType) throws Exception {
		Object value = null;
		if (input.containsKey(name) && !input.isNull(name)) {
			if (String.class.isAssignableFrom(valueType)) {
				value = input.getString(name);
			} else if (Boolean.class.isAssignableFrom(valueType)) {
				value = input.getBoolean(name);
			} else if (Integer.class.isAssignableFrom(valueType)) {
				value = input.getInt(name);
			} else if (Double.class.isAssignableFrom(valueType)) {
				value = input.getJsonNumber(name).doubleValue();
			} else if (Long.class.isAssignableFrom(valueType)) {
				value = input.getJsonNumber(name).longValue();
			} else if (BigDecimal.class.isAssignableFrom(valueType)) {
				value = input.getJsonNumber(name).bigDecimalValue();
			} else if (BigInteger.class.isAssignableFrom(valueType)) {
				value = input.getJsonNumber(name).bigIntegerValue();
			} else {
				// complex object with nested fields
				if(input.containsKey(name) && !input.isNull(name)){
					value = valueType.getConstructor().newInstance();

					JsonObject nestedObjectFromInput = input.getJsonObject(name);

					Field[] fields = FieldUtils.getAllFields(value.getClass());
					for (Field field : fields) {
						if (field.isAnnotationPresent(step.handlers.javahandler.Input.class)) {
							step.handlers.javahandler.Input fieldAnnotation = field.getAnnotation(step.handlers.javahandler.Input.class);
							String jsonNameForField = fieldAnnotation.name() == null || fieldAnnotation.name().isEmpty() ? field.getName() : fieldAnnotation.name();

							FieldUtils.writeField(field, value, getValueFromJsonInput(nestedObjectFromInput, jsonNameForField, field.getType()), true);
						}
					}
				}

			}
		}

		// TODO: arrays?
		return value;
	}

}
