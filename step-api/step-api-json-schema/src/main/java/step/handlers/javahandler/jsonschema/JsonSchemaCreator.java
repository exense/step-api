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

public class JsonSchemaCreator {

    private final JsonProvider jsonProvider;
    private final JsonSchemaFieldProcessor customFieldProcessor;
    private final JsonSchemaFieldProcessor defaultFieldProcessor;
    private final FieldMetadataExtractor metadataExtractor;

    public JsonSchemaCreator(JsonProvider jsonProvider, JsonSchemaFieldProcessor customFieldProcessor, FieldMetadataExtractor metadataExtractor) {
        this.jsonProvider = jsonProvider;
        this.customFieldProcessor = customFieldProcessor;
        this.metadataExtractor = metadataExtractor;
        this.defaultFieldProcessor = new DefaultJsonSchemaFieldProcessor();
    }

    public void processNestedFields(JsonObjectBuilder propertyParamsBuilder, Class<?> clazz) throws JsonSchemaPreparationException {
        List<String> requiredProperties = new ArrayList<>();
        List<Field> fields = step.handlers.javahandler.JsonInputConverter.getAllFields(clazz);

        JsonObjectBuilder nestedPropertiesBuilder = jsonProvider.createObjectBuilder();
        processFields(clazz, nestedPropertiesBuilder, fields, requiredProperties);
        propertyParamsBuilder.add("properties", nestedPropertiesBuilder);

        if (!requiredProperties.isEmpty()) {
            JsonArrayBuilder requiredBuilder = jsonProvider.createArrayBuilder();
            for (String requiredProperty : requiredProperties) {
                requiredBuilder.add(requiredProperty);
            }
            propertyParamsBuilder.add("required", requiredBuilder);
        }
    }

    public void processFields(Class<?> objectClass,
                              JsonObjectBuilder propertiesBuilder,
                              List<Field> fields,
                              List<String> requiredPropertiesOutput) throws JsonSchemaPreparationException {
        for (Field field : fields) {
            FieldMetadata fieldMetadata = metadataExtractor.extractMetadata(objectClass, field);

            // try to apply custom logic for field
            if (!customFieldProcessor.applyCustomProcessing(objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput, this)) {
                defaultFieldProcessor.applyCustomProcessing(objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput, this);
            }

            // apply "required" fields to json schema
            if (fieldMetadata.isRequired()) {
                requiredPropertiesOutput.add(fieldMetadata.getFieldName());
            }

        }
    }

    public static void addDefaultValue(String defaultValue, JsonObjectBuilder builder, Type type, String paramName) throws JsonSchemaPreparationException {
        try {
            JsonInputConverter.addValueToJsonBuilder(defaultValue, builder, type, "default");
        } catch (IllegalArgumentException ex) {
            throw new JsonSchemaPreparationException("Unable to resolve default value for input " + paramName +
                    ". Caused by : " + ex.getMessage());
        }
    }

    public JsonProvider getJsonProvider() {
        return jsonProvider;
    }
}
