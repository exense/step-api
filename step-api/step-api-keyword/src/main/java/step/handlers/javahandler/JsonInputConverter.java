/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.handlers.javahandler;

import javax.json.*;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class JsonInputConverter {

	public static final String ARRAY_VALUE_SEPARATOR = ";";

	public static Object getValueFromJsonInput(JsonObject input, String name, String defaultValue, Type type) throws Exception {
		Class<?> valueType = null;

		try {
			if (type instanceof Class) {
				valueType = (Class<?>) type;
			} else if (type instanceof ParameterizedType) {
				// we expect the parameterized collection here
				valueType = (Class<?>) ((ParameterizedType) type).getRawType();
			} else {
				throw new IllegalArgumentException("Unsupported type " + type + " found for field " + name);
			}
		} catch (Exception ex) {
			throw new IllegalArgumentException("Unsupported type " + type + " found for field " + name);
		}

		Object value = null;
		boolean validInputValue = (input.containsKey(name) && !input.isNull(name));
		boolean validDefaultValue = defaultValue != null && !defaultValue.isEmpty();
		if (validInputValue || validDefaultValue) {
			Optional<Object> valueFrom = getSimpleValueFrom(input, name, valueType, defaultValue, validInputValue);
			if (valueFrom.isPresent()) {
				value = valueFrom.get();
			} else if (valueType.isArray() || Collection.class.isAssignableFrom(valueType)) {
				JsonArray jsonArray = (validInputValue) ? input.getJsonArray(name) :
						convertStringToJsonArrayBuilder(name, defaultValue, valueType, type).build();
				Class<?> arrayValueType;
				if (valueType.isArray()) {
					arrayValueType = valueType.getComponentType();
				} else if (Collection.class.isAssignableFrom(valueType)) {
					// we need to check the generic parameter type for collection
					arrayValueType = resolveGenericTypeForCollection(type, name);
				} else {
					throw new IllegalArgumentException("Unsupported type found for array input " + name + ": " + type);
				}
				Object[] arrayValue = null;
				if (String.class.isAssignableFrom(arrayValueType)) {
					arrayValue = new String[jsonArray.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						arrayValue[i] = jsonArray.getString(i);
					}
				} else if (Boolean.class.isAssignableFrom(arrayValueType) || arrayValueType.equals(boolean.class)) {
					arrayValue = new Boolean[jsonArray.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						arrayValue[i] = jsonArray.getBoolean(i);
					}
				} else if (Integer.class.isAssignableFrom(arrayValueType) || arrayValueType.equals(int.class)) {
					arrayValue = new Integer[jsonArray.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						arrayValue[i] = jsonArray.getInt(i);
					}
				} else if (Double.class.isAssignableFrom(arrayValueType) || arrayValueType.equals(double.class)) {
					arrayValue = new Double[jsonArray.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						arrayValue[i] = jsonArray.getJsonNumber(i).doubleValue();
					}
				} else if (Long.class.isAssignableFrom(arrayValueType) || arrayValueType.equals(long.class)) {
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
				if (Collection.class.isAssignableFrom(valueType)) {
					value = valueType.getConstructor().newInstance();
					((Collection) value).addAll(Arrays.asList(arrayValue));
				} else {
					value = arrayValue;
				}
			} else if (Map.class.isAssignableFrom(valueType) && type instanceof ParameterizedType) {
				Map map;
				//If it is declared with a Map interface, default to HashMap
				if (valueType.isInterface()) {
					map = new HashMap<>();
				} else {
					map = (Map) valueType.getConstructor().newInstance();
				}
				Type actualTypeArgument = ((ParameterizedType) type).getActualTypeArguments()[1];
				JsonObject nestedObjectFromInput = (validInputValue) ? input.getJsonObject(name) :
						Json.createReader(new StringReader(defaultValue)).readObject();
				for (String jsonNameForField: nestedObjectFromInput.keySet()) {
					Object valueFromJsonInput = getValueFromJsonInput(nestedObjectFromInput, jsonNameForField, null, actualTypeArgument);
					map.put(jsonNameForField, valueFromJsonInput);
				}
				value = map;
			} else {
				// complex object with nested fields
				value = valueType.getConstructor().newInstance();

				JsonObject nestedObjectFromInput = (validInputValue) ? input.getJsonObject(name) :
						Json.createReader(new StringReader(defaultValue)).readObject();

				List<Field> fields = getAllFields(value.getClass());
				for (Field field : fields) {
					String jsonNameForField = field.getName();
					String nestedDefaultValue = null;
					if (field.isAnnotationPresent(step.handlers.javahandler.Input.class)) {
						step.handlers.javahandler.Input fieldAnnotation = field.getAnnotation(step.handlers.javahandler.Input.class);
						jsonNameForField = fieldAnnotation.name() == null || fieldAnnotation.name().isEmpty() ? field.getName() : fieldAnnotation.name();
						nestedDefaultValue = fieldAnnotation.required() ? null : fieldAnnotation.defaultValue();
					}
					writeField(field, value, getValueFromJsonInput(nestedObjectFromInput, jsonNameForField, nestedDefaultValue, field.getType()));
				}

			}
		}

		return value;
	}


	private static Optional<Object> getSimpleValueFrom(JsonObject input, String name, Class<?> valueType, String defaultValue, boolean validInputValue) {
		Object result = null;
		if (String.class.isAssignableFrom(valueType)) {
			result = (validInputValue) ? input.getString(name) : defaultValue;
		} else if (Boolean.class.isAssignableFrom(valueType) || valueType.equals(boolean.class)) {
			result = (validInputValue) ? input.getBoolean(name) : Boolean.parseBoolean(defaultValue);
		} else if (Integer.class.isAssignableFrom(valueType) || valueType.equals(int.class)) {
			result = (validInputValue) ? input.getInt(name) : Integer.parseInt(defaultValue);
		} else if (Double.class.isAssignableFrom(valueType) || valueType.equals(double.class)) {
			result = (validInputValue) ? input.getJsonNumber(name).doubleValue() : Double.parseDouble(defaultValue);
		} else if (Long.class.isAssignableFrom(valueType) || valueType.equals(long.class)) {
			result = (validInputValue) ? input.getJsonNumber(name).longValue() : Long.parseLong(defaultValue);
		} else if (BigDecimal.class.isAssignableFrom(valueType)) {
			result = (validInputValue) ? input.getJsonNumber(name).bigDecimalValue() : new BigDecimal(defaultValue);
		} else if (BigInteger.class.isAssignableFrom(valueType)) {
			result = (validInputValue) ? input.getJsonNumber(name).bigIntegerValue() : new BigInteger(defaultValue);
		}
		return Optional.ofNullable(result);
	}

	private static JsonArrayBuilder convertStringToJsonArrayBuilder(String jsonName, String value, Class<?> clazz, Type type) {
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

		Class<?> arrayValueType;
		if (clazz.isArray()) {
			arrayValueType = clazz.getComponentType();
		} else if (Collection.class.isAssignableFrom(clazz)) {
			// we need to check the generic parameter type for collection
			arrayValueType = resolveGenericTypeForCollection(type, jsonName);
		} else {
			throw new IllegalArgumentException("Unsupported type found for array field " + jsonName + ": " + type);
		}

		for (String arrayValue : value.split(ARRAY_VALUE_SEPARATOR)) {
			if(String.class.isAssignableFrom(arrayValueType)){
				arrayBuilder.add(arrayValue);
			} else if(Boolean.class.isAssignableFrom(arrayValueType) || arrayValueType.equals(boolean.class)){
				arrayBuilder.add(Boolean.parseBoolean(arrayValue));
			} else if(Integer.class.isAssignableFrom(arrayValueType) || arrayValueType.equals(int.class)){
				arrayBuilder.add(Integer.parseInt(arrayValue));
			} else if(Long.class.isAssignableFrom(arrayValueType) || arrayValueType.equals(long.class)){
				arrayBuilder.add(Long.parseLong(arrayValue));
			} else if(Double.class.isAssignableFrom(arrayValueType) || arrayValueType.equals(double.class)){
				arrayBuilder.add(Double.parseDouble(arrayValue));
			} else if(BigInteger.class.isAssignableFrom(arrayValueType)){
				arrayBuilder.add(BigInteger.valueOf(Long.parseLong(arrayValue)));
			} else if(BigDecimal.class.isAssignableFrom(arrayValueType)) {
				arrayBuilder.add(BigDecimal.valueOf(Double.parseDouble(arrayValue)));
			} else {
				throw new IllegalArgumentException("Unsupported type found for array field " + jsonName + ": " + arrayValueType);
			}
		}
		return arrayBuilder;
	}

	public static Class<?> resolveGenericTypeForCollection(Type type, String jsonName) {
		Class<?> arrayValueType;
		if (!(type instanceof ParameterizedType)) {
			throw new IllegalArgumentException("Unsupported type found for array field " + jsonName + ": " + type);
		}

		Type[] collectionGenerics = ((ParameterizedType) type).getActualTypeArguments();
		if (collectionGenerics.length != 1) {
			throw new IllegalArgumentException("Unsupported type found for array field " + jsonName + ": " + type);
		}

		Type genericType = collectionGenerics[0];
		if (!(genericType instanceof Class)) {
			throw new IllegalArgumentException("Unsupported type found for array field " + jsonName + ": " + type);
		}
		arrayValueType = (Class<?>) genericType;
		return arrayValueType;
	}

	public static List<Field> getAllFields(final Class<?> cls) {
		final List<Field> allFields = new ArrayList<>();
		Class<?> currentClass = cls;
		while (currentClass != null) {
			final Field[] declaredFields = currentClass.getDeclaredFields();
			Collections.addAll(allFields, declaredFields);
			currentClass = currentClass.getSuperclass();
		}
		return allFields.stream().filter(f -> !f.isSynthetic()).collect(Collectors.toList());
	}

	public static void writeField(final Field field, final Object target, final Object value) throws IllegalAccessException {
		if (!Modifier.isStatic(field.getModifiers())) {
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}
			field.set(target, value);
		}
	}

}
