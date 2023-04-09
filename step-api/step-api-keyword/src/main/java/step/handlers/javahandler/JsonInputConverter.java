package step.handlers.javahandler;

import org.apache.commons.lang3.reflect.FieldUtils;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonInputConverter {

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
