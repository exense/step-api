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

import jakarta.json.*;

import java.io.StringReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import static step.handlers.javahandler.JsonInputConverter.resolveGenericTypeForArrayAndCollection;
import static step.handlers.javahandler.JsonObjectMapper.getTypeClass;
import static step.handlers.javahandler.JsonObjectMapper.resolveGenericTypeForCollection;

public class JsonInputConverter {

	public static final String ARRAY_VALUE_SEPARATOR = ";";

	// TODO this method duplicates a lot of the code of JsonObjectMapper. It should be refactored
	public static void addValueToJsonBuilder(String value, JsonObjectBuilder builder, Type type, String jsonName) throws IllegalArgumentException {
		Class<?> clazz = resolveClass(type, jsonName);

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

			Class<?> arrayValueType = resolveGenericTypeForArrayAndCollection(clazz, type, jsonName);

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
			//assuming json object as json string
			try (JsonReader jsonReader = Json.createReader(new StringReader(value))) {
				JsonObject jsonObject = jsonReader.readObject();
				builder.add(jsonName, jsonObject);
			} catch (Exception e) {
				throw new IllegalArgumentException("Unsupported type found for field " + jsonName + ": " + type);
			}
		}
	}

	public static Class resolveClass(Type type, String jsonName) {
		Class<?> clazz;
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
		return clazz;
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
