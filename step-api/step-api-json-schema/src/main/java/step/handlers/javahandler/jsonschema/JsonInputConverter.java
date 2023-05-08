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
package step.handlers.javahandler.jsonschema;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import static step.handlers.javahandler.JsonInputConverter.resolveGenericTypeForCollection;

public class JsonInputConverter {

	public static final String ARRAY_VALUE_SEPARATOR = ";";

	public static void addValueToJsonBuilder(String value, JsonObjectBuilder builder, Type type, String jsonName) throws IllegalArgumentException {
		Class<?> clazz = null;

		try {
			if (type instanceof Class) {
				clazz = (Class<?>) type;
			} else if (type instanceof ParameterizedType) {
				// we expect the parameterized collection here
				clazz = (Class<?>) ((ParameterizedType) type).getRawType();
			} else {
				throw new IllegalArgumentException("Unsupported type " + type + " found for field " + jsonName);
			}
		} catch (Exception ex) {
			throw new IllegalArgumentException("Unsupported type " + type + " found for field " + jsonName);
		}

		if(String.class.isAssignableFrom(clazz)){
			builder.add(jsonName, value);
		} else if(Boolean.class.isAssignableFrom(clazz) || clazz.equals(boolean.class)){
			builder.add(jsonName, Boolean.parseBoolean(value));
		} else if(Integer.class.isAssignableFrom(clazz) || clazz.equals(int.class)){
			builder.add(jsonName, Integer.parseInt(value));
		} else if(Long.class.isAssignableFrom(clazz) || clazz.equals(long.class)){
			builder.add(jsonName, Long.parseLong(value));
		} else if(Double.class.isAssignableFrom(clazz) || clazz.equals(double.class)){
			builder.add(jsonName, Double.parseDouble(value));
		} else if(BigInteger.class.isAssignableFrom(clazz)){
			builder.add(jsonName, BigInteger.valueOf(Long.parseLong(value)));
		} else if(BigDecimal.class.isAssignableFrom(clazz)){
			builder.add(jsonName, BigDecimal.valueOf(Double.parseDouble(value)));
		} else if(clazz.isArray() || Collection.class.isAssignableFrom(clazz)){
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
			builder.add(jsonName, arrayBuilder);
		} else {
			throw new IllegalArgumentException("Unsupported type found for field " + jsonName + ": " + type);
		}
	}



	public static String resolveJsonPropertyType(Class<?> type) {
		if (String.class.isAssignableFrom(type)) {
			return "string";
		} else if (Boolean.class.isAssignableFrom(type) || type.equals(boolean.class)) {
			return "boolean";
		} else if (Number.class.isAssignableFrom(type) || type.equals(int.class) || type.equals(long.class) || type.equals(double.class)) {
			return "number";
		} else if (type.isArray() || Collection.class.isAssignableFrom(type)){
			return "array";
		} else {
			return "object";
		}
	}


}
