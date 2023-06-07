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
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JsonSchemaCreator {

    private final JsonProvider jsonProvider;
    private final JsonSchemaFieldProcessor customFieldProcessor;
    private final FieldMetadataExtractor metadataExtractor;

    public JsonSchemaCreator(JsonProvider jsonProvider, JsonSchemaFieldProcessor customFieldProcessor, FieldMetadataExtractor metadataExtractor) {
        this.jsonProvider = jsonProvider;
        this.customFieldProcessor = customFieldProcessor;
        this.metadataExtractor = metadataExtractor;
    }

    public void processNestedFields(JsonObjectBuilder propertyParamsBuilder, Class<?> clazz) throws JsonSchemaPreparationException {
        List<String> requiredProperties = new ArrayList<>();
        List<Field> fields = step.handlers.javahandler.JsonInputConverter.getAllFields(clazz);

        JsonObjectBuilder nestedPropertiesBuilder = jsonProvider.createObjectBuilder();
        processFields(nestedPropertiesBuilder, fields, requiredProperties);
        propertyParamsBuilder.add("properties", nestedPropertiesBuilder);

        if (!requiredProperties.isEmpty()) {
            JsonArrayBuilder requiredBuilder = jsonProvider.createArrayBuilder();
            for (String requiredProperty : requiredProperties) {
                requiredBuilder.add(requiredProperty);
            }
            propertyParamsBuilder.add("required", requiredBuilder);
        }
    }

    public void processFields(JsonObjectBuilder nestedPropertiesBuilder,
                              List<Field> fields,
                              List<String> requiredPropertiesOutput) throws JsonSchemaPreparationException {
        for (Field field : fields) {
            // to avoid processing technical fields like $jacoco
            if (customFieldProcessor.skipField(field)) {
                continue;
            }

            JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();

            // extract field parameters (name, required, default value etc)
            FieldMetadata fieldMetadata = metadataExtractor.extractMetadata(field);
            String type = JsonInputConverter.resolveJsonPropertyType(fieldMetadata.getType());
            String parameterName = fieldMetadata.getFieldName();
            if (fieldMetadata.isRequired()) {
                requiredPropertiesOutput.add(parameterName);
            }
            if (fieldMetadata.getDefaultValue() != null) {
                addDefaultValue(fieldMetadata.getDefaultValue(), nestedPropertyParamsBuilder, fieldMetadata.getType(), parameterName);
            }

            if (Objects.equals("object", type)) {
                // for object type apply some logic to resolve nested fields
                if (!customFieldProcessor.applyCustomProcessing(field, nestedPropertyParamsBuilder)) {
                    // if there is no custom logic for this field - just process nested fields recursively by default
                    nestedPropertyParamsBuilder.add("type", type);

                    // apply some custom logic for field or use the default behavior - process nested fields recursively
                    processNestedFields(nestedPropertyParamsBuilder, field.getType());
                }
            } else {
                // for simple types just add a "type" to json schema
                nestedPropertyParamsBuilder.add("type", type);
            }

            nestedPropertiesBuilder.add(parameterName, nestedPropertyParamsBuilder);
        }
    }

    public void addDefaultValue(String defaultValue, JsonObjectBuilder builder, Type type, String paramName) throws JsonSchemaPreparationException {
        try {
            JsonInputConverter.addValueToJsonBuilder(defaultValue, builder, type, "default");
        } catch (IllegalArgumentException ex) {
            throw new JsonSchemaPreparationException("Unable to resolve default value for input " + paramName +
                    ". Caused by : " + ex.getMessage());
        }
    }


}
