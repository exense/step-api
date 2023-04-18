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


}
