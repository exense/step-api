package step.handlers.javahandler;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonInputConverter {

	public static final String ARRAY_VALUE_SEPARATOR = ";";

	public static void addValueToJsonBuilder(String value, JsonObjectBuilder builder, Class<?> type, String jsonName) throws IllegalArgumentException {
		if(String.class.isAssignableFrom(type)){
			builder.add(jsonName, value);
		} else if(Boolean.class.isAssignableFrom(type)){
			builder.add(jsonName, Boolean.parseBoolean(value));
		} else if(Integer.class.isAssignableFrom(type)){
			builder.add(jsonName, Integer.parseInt(value));
		} else if(Long.class.isAssignableFrom(type)){
			builder.add(jsonName, Long.parseLong(value));
		} else if(Double.class.isAssignableFrom(type)){
			builder.add(jsonName, Double.parseDouble(value));
		} else if(BigInteger.class.isAssignableFrom(type)){
			builder.add(jsonName, BigInteger.valueOf(Long.parseLong(value)));
		} else if(BigDecimal.class.isAssignableFrom(type)){
			builder.add(jsonName, BigDecimal.valueOf(Double.parseDouble(value)));
		} else if(type.isArray()){
			JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
			for (String arrayValue : value.split(ARRAY_VALUE_SEPARATOR)) {
				Class<?> arrayValueType = type.getComponentType();
				if(String.class.isAssignableFrom(arrayValueType)){
					arrayBuilder.add(arrayValue);
				} else if(Boolean.class.isAssignableFrom(arrayValueType)){
					arrayBuilder.add(Boolean.parseBoolean(arrayValue));
				} else if(Integer.class.isAssignableFrom(arrayValueType)){
					arrayBuilder.add(Integer.parseInt(arrayValue));
				} else if(Long.class.isAssignableFrom(arrayValueType)){
					arrayBuilder.add(Long.parseLong(arrayValue));
				} else if(Double.class.isAssignableFrom(arrayValueType)){
					arrayBuilder.add(Double.parseDouble(arrayValue));
				} else if(BigInteger.class.isAssignableFrom(arrayValueType)){
					arrayBuilder.add(BigInteger.valueOf(Long.parseLong(arrayValue)));
				} else if(BigDecimal.class.isAssignableFrom(arrayValueType)) {
					arrayBuilder.add(BigDecimal.valueOf(Double.parseDouble(arrayValue)));
				} else {
					throw new IllegalArgumentException("Unable to apply the following type to array json builder: " + type);
				}
			}
			builder.add(jsonName, arrayBuilder);
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
		} else if (type.isArray()){
			return "array";
		} else {
			return "object";
		}
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
			} else if (valueType.isArray()) {
				JsonArray jsonArray = input.getJsonArray(name);
				Class<?> arrayValueType = valueType.getComponentType();
				Object[] arrayValue = null;
				if (String.class.isAssignableFrom(arrayValueType)) {
					arrayValue = new String[jsonArray.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						arrayValue[i] = jsonArray.getString(i);
					}
				} else if (Boolean.class.isAssignableFrom(arrayValueType)) {
					arrayValue = new Boolean[jsonArray.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						arrayValue[i] = jsonArray.getBoolean(i);
					}
				} else if (Integer.class.isAssignableFrom(arrayValueType)) {
					arrayValue = new Integer[jsonArray.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						arrayValue[i] = jsonArray.getInt(i);
					}
				} else if (Double.class.isAssignableFrom(arrayValueType)) {
					arrayValue = new Double[jsonArray.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						arrayValue[i] = jsonArray.getJsonNumber(i).doubleValue();
					}
				} else if (Long.class.isAssignableFrom(arrayValueType)) {
					arrayValue = new Long[jsonArray.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						arrayValue[i] = jsonArray.getJsonNumber(i).longValue();
					}
				} else if (BigDecimal.class.isAssignableFrom(arrayValueType)) {
					arrayValue = new BigDecimal[jsonArray.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						arrayValue[i] = jsonArray.getJsonNumber(i).bigDecimalValue();
					}
				} else if (BigInteger.class.isAssignableFrom(arrayValueType)) {
					arrayValue = new BigInteger[jsonArray.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						arrayValue[i] = jsonArray.getJsonNumber(i).bigIntegerValue();
					}
				}

				value = arrayValue;
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

		return value;
	}

}
