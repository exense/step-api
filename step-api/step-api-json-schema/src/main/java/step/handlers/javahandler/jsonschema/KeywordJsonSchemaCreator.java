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

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonParsingException;
import step.handlers.javahandler.Input;
import step.handlers.javahandler.Keyword;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class KeywordJsonSchemaCreator {

	private final JsonProvider jsonProvider = JsonProvider.provider();

	/**
	 * Creates a json schema for java method annotated with {@link Keyword} annotation
	 *
	 * @throws JsonSchemaPreparationException
	 */
	public JsonObject createJsonSchemaForKeyword(Method method) throws JsonSchemaPreparationException{
		Keyword keywordAnnotation = method.getAnnotation(Keyword.class);
		if (keywordAnnotation == null) {
			throw new JsonSchemaPreparationException("Method is not annotated with Keyword annotation");
		}
		String functionName = keywordAnnotation.name().length()>0?keywordAnnotation.name():method.getName();

		// use explicit (plain text) schema specified in annotation
		boolean useTextJsonSchema = keywordAnnotation.schema() != null && !keywordAnnotation.schema().isEmpty();

		// build json schema via @Input annotations taken from method parameters
		boolean useAnnotatedJsonInputs = method.getParameters() != null && Arrays.stream(method.getParameters()).anyMatch(p -> p.isAnnotationPresent(Input.class));
		if (useTextJsonSchema && useAnnotatedJsonInputs) {
			throw new IllegalArgumentException("Ambiguous definition of json schema for keyword '" + functionName +"'. You should use either '@Input' annotation or define the 'schema' element of the @Keyword annotation");
		}

		if (useTextJsonSchema) {
			return readJsonSchemaFromPlainText(keywordAnnotation.schema(), method);
		} else if (useAnnotatedJsonInputs) {
			return readJsonSchemaFromInputAnnotations(method);
		} else {
			return createEmptyJsonSchema();
		}
	}

	private JsonObject readJsonSchemaFromInputAnnotations(Method method) throws JsonSchemaPreparationException {
		Keyword keywordAnnotation = method.getAnnotation(Keyword.class);
		String functionName = keywordAnnotation.name().length()>0?keywordAnnotation.name():method.getName();
		JsonObjectBuilder topLevelBuilder = jsonProvider.createObjectBuilder();
		// top-level type is always 'object'
		topLevelBuilder.add("type", "object");

		JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
		List<String> requiredProperties = new ArrayList<>();
		for (Parameter p : method.getParameters()) {
			JsonObjectBuilder propertyParamsBuilder = jsonProvider.createObjectBuilder();

			if (!p.isAnnotationPresent(Input.class)) {
				throw new JsonSchemaPreparationException("Parameter " + p.getName() + " is not annotated with " + Input.class.getName() + " for keyword " + functionName);
			}

			Input inputAnnotation = p.getAnnotation(Input.class);

			String parameterName = inputAnnotation.name();
			if(parameterName == null || parameterName.isEmpty()){
				throw new JsonSchemaPreparationException("The mandatory 'name' element of the Input annotation is missing for parameter " + p.getName() + " in keyword " + functionName);
			}

			Class<?> type1 = p.getType();
			String type = JsonInputConverter.resolveJsonPropertyType(type1);
			propertyParamsBuilder.add("type", type);

			if (inputAnnotation.defaultValue() != null && !inputAnnotation.defaultValue().isEmpty()) {
				try {
					addDefaultValue(inputAnnotation.defaultValue(), propertyParamsBuilder, p.getType(), parameterName);
				} catch (JsonSchemaPreparationException e) {
					throw new JsonSchemaPreparationException("Schema creation error for keyword '"
							+ functionName + "': " + e.getMessage());
				}
			}

			if (inputAnnotation.required()) {
				requiredProperties.add(parameterName);
			}

			if(Objects.equals("object", type)){
				try {
					processNestedFields(propertyParamsBuilder, p.getType());
				} catch (JsonSchemaPreparationException e) {
					throw new JsonSchemaPreparationException("Schema creation error for keyword '"
							+ functionName + "': " + e.getMessage());
				}

			}

			propertiesBuilder.add(parameterName, propertyParamsBuilder);
		}
		topLevelBuilder.add("properties", propertiesBuilder);

		JsonArrayBuilder requiredBuilder = jsonProvider.createArrayBuilder();
		for (String requiredProperty : requiredProperties) {
			requiredBuilder.add(requiredProperty);
		}
		topLevelBuilder.add("required", requiredBuilder);
		return topLevelBuilder.build();
	}

	private void processNestedFields(JsonObjectBuilder propertyParamsBuilder, Class<?> clazz) throws JsonSchemaPreparationException {
		JsonObjectBuilder nestedPropertiesBuilder = jsonProvider.createObjectBuilder();
		List<String> requiredProperties = new ArrayList<>();

		List<Field> fields = step.handlers.javahandler.JsonInputConverter.getAllFields(clazz);
		for (Field field : fields) {
			JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();

			if (field.isAnnotationPresent(Input.class)) {
				Input input = field.getAnnotation(Input.class);
				String parameterName = input.name() == null || input.name().isEmpty() ? field.getName() : input.name();

				if (input.required()) {
					requiredProperties.add(parameterName);
				}

				String type = JsonInputConverter.resolveJsonPropertyType(field.getType());
				nestedPropertyParamsBuilder.add("type", type);

				if (input.defaultValue() != null && !input.defaultValue().isEmpty()) {
					addDefaultValue(input.defaultValue(), nestedPropertyParamsBuilder, field.getType(), parameterName);
				}

				if (Objects.equals("object", type)) {
					processNestedFields(nestedPropertyParamsBuilder, field.getType());
				}

				nestedPropertiesBuilder.add(parameterName, nestedPropertyParamsBuilder);
			}

		}
		propertyParamsBuilder.add("properties", nestedPropertiesBuilder);

		JsonArrayBuilder requiredBuilder = jsonProvider.createArrayBuilder();
		for (String requiredProperty : requiredProperties) {
			requiredBuilder.add(requiredProperty);
		}
		propertyParamsBuilder.add("required", requiredBuilder);
	}

	private void addDefaultValue(String defaultValue, JsonObjectBuilder builder, Class<?> type, String paramName) throws JsonSchemaPreparationException {
		try {
			JsonInputConverter.addValueToJsonBuilder(defaultValue, builder, type, "default");
		} catch (IllegalArgumentException ex) {
			throw new JsonSchemaPreparationException("Unable to resolve default value for input " + paramName +
					". Caused by : " + ex.getMessage());
		}
	}

	protected JsonObject createEmptyJsonSchema() {
		return jsonProvider.createObjectBuilder().build();
	}

	protected JsonObject readJsonSchemaFromPlainText(String schema, Method method) throws JsonSchemaPreparationException {
		try {
			return jsonProvider.createReader(new StringReader(schema)).readObject();
		} catch (JsonParsingException e) {
			throw new JsonSchemaPreparationException("Parsing error in the schema for keyword '" + method.getName() + "'. The error was: " + e.getMessage());
		} catch (JsonException e) {
			throw new JsonSchemaPreparationException("I/O error in the schema for keyword '" + method.getName() + "'. The error was: " + e.getMessage());
		} catch (Exception e) {
			throw new JsonSchemaPreparationException("Unknown error in the schema for keyword '" + method.getName() + "'. The error was: " + e.getMessage());
		}
	}
}
