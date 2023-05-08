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

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JsonInputConverter {

	public static Object getValueFromJsonInput(JsonObject input, String name, Type type) throws Exception {
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
		if (input.containsKey(name) && !input.isNull(name)) {
			if (String.class.isAssignableFrom(valueType)) {
				value = input.getString(name);
			} else if (Boolean.class.isAssignableFrom(valueType) || valueType.equals(boolean.class)) {
				value = input.getBoolean(name);
			} else if (Integer.class.isAssignableFrom(valueType) || valueType.equals(int.class)) {
				value = input.getInt(name);
			} else if (Double.class.isAssignableFrom(valueType) || valueType.equals(double.class)) {
				value = input.getJsonNumber(name).doubleValue();
			} else if (Long.class.isAssignableFrom(valueType) || valueType.equals(long.class)) {
				value = input.getJsonNumber(name).longValue();
			} else if (BigDecimal.class.isAssignableFrom(valueType)) {
				value = input.getJsonNumber(name).bigDecimalValue();
			} else if (BigInteger.class.isAssignableFrom(valueType)) {
				value = input.getJsonNumber(name).bigIntegerValue();
			} else if (valueType.isArray() || Collection.class.isAssignableFrom(valueType)) {
				JsonArray jsonArray = input.getJsonArray(name);
				Class<?> arrayValueType = valueType.getComponentType();
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
				} else if (Boolean.class.isAssignableFrom(arrayValueType) || valueType.equals(boolean.class)) {
					arrayValue = new Boolean[jsonArray.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						arrayValue[i] = jsonArray.getBoolean(i);
					}
				} else if (Integer.class.isAssignableFrom(arrayValueType) || valueType.equals(int.class)) {
					arrayValue = new Integer[jsonArray.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						arrayValue[i] = jsonArray.getInt(i);
					}
				} else if (Double.class.isAssignableFrom(arrayValueType) || valueType.equals(double.class)) {
					arrayValue = new Double[jsonArray.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						arrayValue[i] = jsonArray.getJsonNumber(i).doubleValue();
					}
				} else if (Long.class.isAssignableFrom(arrayValueType) || valueType.equals(long.class)) {
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
			} else {
				// complex object with nested fields
				if(input.containsKey(name) && !input.isNull(name)){
					value = valueType.getConstructor().newInstance();

					JsonObject nestedObjectFromInput = input.getJsonObject(name);

					List<Field> fields = getAllFields(value.getClass());
					for (Field field : fields) {
						String jsonNameForField = field.getName();
						if (field.isAnnotationPresent(step.handlers.javahandler.Input.class)) {
							step.handlers.javahandler.Input fieldAnnotation = field.getAnnotation(step.handlers.javahandler.Input.class);
							jsonNameForField = fieldAnnotation.name() == null || fieldAnnotation.name().isEmpty() ? field.getName() : fieldAnnotation.name();
						}
						writeField(field, value, getValueFromJsonInput(nestedObjectFromInput, jsonNameForField, field.getType()));
					}
				}

			}
		}

		return value;
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
		if (!field.isAccessible()) {
			field.setAccessible(true);
		}
		field.set(target, value);
	}

}
