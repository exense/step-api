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

import jakarta.json.JsonObjectBuilder;

import java.lang.reflect.Field;
import java.util.List;

/**
 * The logic of json schema generation for some field in java object
 */
public interface JsonSchemaFieldProcessor {

	/**
	 * Applies non-default JSON schema preparation logic for the field
	 *
	 * @param objectClass the field owner class
	 * @param field the target field
	 * @param fieldMetadata field metadata containing the information about field name, default value etc
	 * @param propertiesBuilder json object builder to be filled with field data
	 * @param requiredPropertiesOutput for required fields the field name should be added
	 * @return true - custom processing is applied, false - custom processing is not required
	 */
	default boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput) throws JsonSchemaPreparationException {
		return false;
	}

}
