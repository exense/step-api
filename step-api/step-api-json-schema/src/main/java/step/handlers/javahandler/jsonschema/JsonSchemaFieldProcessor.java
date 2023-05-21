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

/**
 * The logic of json schema generation for some field in java object
 */
public interface JsonSchemaFieldProcessor {

	/**
	 * @return true - custom processing is applied, false - custom processing is not required
	 */
	default boolean applyCustomProcessing(Field field, JsonObjectBuilder propertiesBuilder) {
		return false;
	}

	/**
	 * @return true if the field should NOT be included in json schema
	 */
	default boolean skipField(Field field) {
		return false;
	}
}
