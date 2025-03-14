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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;

import static step.handlers.javahandler.JsonObjectMapper.getTypeClass;
import static step.handlers.javahandler.JsonObjectMapper.resolveGenericTypeForArrayOrCollection;

public class KeywordJsonSchemaCreator {

	private final JsonProvider jsonProvider = JsonProvider.provider();

	private final JsonSchemaCreator jsonSchemaCreator = new JsonSchemaCreator(
			jsonProvider,
			(objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput, schemaCreator) -> false,
			new KeywordInputMetadataExtractor()
	);

	/**
	 * Creates a json schema for java method annotated with {@link Keyword} annotation
	 */
	public JsonObject createJsonSchemaForKeyword(Method method) throws JsonSchemaPreparationException{
		Keyword keywordAnnotation = method.getAnnotation(Keyword.class);
		if (keywordAnnotation == null) {
			throw new JsonSchemaPreparationException("Method is not annotated with Keyword annotation");
		}
		String functionName = keywordAnnotation.name().length() > 0 ? keywordAnnotation.name() : method.getName();

		// use explicit (plain text) schema specified in annotation
		boolean useTextJsonSchema = keywordAnnotation.schema() != null && !keywordAnnotation.schema().isEmpty();

		// build json schema via @Input annotations taken from method parameters
		boolean useAnnotatedJsonInputs = method.getParameters() != null && Arrays.stream(method.getParameters()).anyMatch(p -> p.isAnnotationPresent(Input.class));
		if (useTextJsonSchema && useAnnotatedJsonInputs) {
			throw new IllegalArgumentException("Ambiguous definition of json schema for keyword '" + functionName + "'. You should use either '@Input' annotation or define the 'schema' element of the @Keyword annotation");
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
		String functionName = keywordAnnotation.name().length() > 0 ? keywordAnnotation.name() : method.getName();
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

			Class<?> type = p.getType();
			String propertyType = JsonInputConverter.resolveJsonPropertyType(type);
			propertyParamsBuilder.add("type", propertyType);

			if (propertyType.equals("array")) {
				//add items type
				Type parameterizedType = p.getParameterizedType();
				Class<?> aClass = getTypeClass(resolveGenericTypeForArrayOrCollection(parameterizedType));
				String arrayElementType = JsonInputConverter.resolveJsonPropertyType(aClass);
				JsonObject arrayType = jsonProvider.createObjectBuilder().add("type", arrayElementType).build();
				propertyParamsBuilder.add("items", arrayType);
			}

			if (inputAnnotation.defaultValue() != null && !inputAnnotation.defaultValue().isEmpty()) {
				try {
					JsonSchemaCreator.addDefaultValue(inputAnnotation.defaultValue(), propertyParamsBuilder, p.getParameterizedType(), parameterName);
				} catch (JsonSchemaPreparationException e) {
					throw new JsonSchemaPreparationException("Schema creation error for keyword '"
							+ functionName + "': " + e.getMessage());
				}
			}

			if (inputAnnotation.required()) {
				requiredProperties.add(parameterName);
			}

			if(Objects.equals("object", propertyType)) {
				//do not process nested fields for Maps, but add an empty property object
				if (Map.class.isAssignableFrom(type)) {
					propertyParamsBuilder.add("properties", jsonProvider.createObjectBuilder());
				} else {
					try {
						jsonSchemaCreator.processNestedFields(propertyParamsBuilder, p.getType());
					} catch (JsonSchemaPreparationException e) {
						throw new JsonSchemaPreparationException("Schema creation error for keyword '"
								+ functionName + "': " + e.getMessage());
					}
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
