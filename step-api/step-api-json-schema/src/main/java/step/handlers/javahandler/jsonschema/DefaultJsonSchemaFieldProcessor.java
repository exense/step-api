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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DefaultJsonSchemaFieldProcessor implements JsonSchemaFieldProcessor {

    public DefaultJsonSchemaFieldProcessor() {
    }

    @Override
    public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput, JsonSchemaCreator schemaCreator) throws JsonSchemaPreparationException {
        // DEFAULT field processing
        JsonProvider jsonProvider = schemaCreator.getJsonProvider();
        JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();

        // 1. extract field parameters (name, required, default value etc)
        String parameterName = fieldMetadata.getFieldName();

        // TODO: default values should also be applied in all processors, but no in DefaultJsonSchemaFieldProcessor only
        if (fieldMetadata.getDefaultValue() != null) {
            JsonSchemaCreator.addDefaultValue(fieldMetadata.getDefaultValue(), nestedPropertyParamsBuilder, fieldMetadata.getType(), parameterName);
        }

        if (fieldMetadata.getCustomProcessor() != null) {
            // custom processor is used
            fieldMetadata.getCustomProcessor().applyCustomProcessing(objectClass, field, fieldMetadata, nestedPropertyParamsBuilder, requiredPropertiesOutput, schemaCreator);
        } else {
            String type = JsonInputConverter.resolveJsonPropertyType(fieldMetadata.getType());
            // 2. for complex objects iterate through the nested fields
            if (Objects.equals("object", type)) {
                // if there is no custom logic for this field - just process nested fields recursively by default
                nestedPropertyParamsBuilder.add("type", type);

                // apply some custom logic for field or use the default behavior - process nested fields recursively
                processNestedFields(nestedPropertyParamsBuilder, field.getType(), schemaCreator);
            } else if (Objects.equals("array", type)) {
                nestedPropertyParamsBuilder.add("type", "array");
                Class<?> elementType = null;
                try {
                    elementType = step.handlers.javahandler.JsonInputConverter.resolveGenericTypeForCollection(fieldMetadata.getGenericType(), fieldMetadata.getFieldName());
                } catch (Exception ex) {
                    // unresolvable generic type
                }
                if (elementType != null) {
                    String itemType = JsonInputConverter.resolveJsonPropertyType(elementType);
                    nestedPropertyParamsBuilder.add("items", jsonProvider.createObjectBuilder().add("type", itemType));
                }
            } else {
                // 3. for simple types just add a "type" to json schema
                nestedPropertyParamsBuilder.add("type", type);
            }
        }

        // 4. add resolved properties to the schema
        propertiesBuilder.add(parameterName, nestedPropertyParamsBuilder);

        return true;
    }

    public void processNestedFields(JsonObjectBuilder propertyParamsBuilder, Class<?> clazz, JsonSchemaCreator schemaCreator) throws JsonSchemaPreparationException {
        JsonProvider jsonProvider = schemaCreator.getJsonProvider();
        List<String> requiredProperties = new ArrayList<>();
        List<Field> fields = step.handlers.javahandler.JsonInputConverter.getAllFields(clazz);

        JsonObjectBuilder nestedPropertiesBuilder = jsonProvider.createObjectBuilder();
        schemaCreator.processFields(clazz, nestedPropertiesBuilder, fields, requiredProperties);
        propertyParamsBuilder.add("properties", nestedPropertiesBuilder);

        if (!requiredProperties.isEmpty()) {
            JsonArrayBuilder requiredBuilder = jsonProvider.createArrayBuilder();
            for (String requiredProperty : requiredProperties) {
                requiredBuilder.add(requiredProperty);
            }
            propertyParamsBuilder.add("required", requiredBuilder);
        }
    }

}
