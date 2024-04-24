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

import step.handlers.javahandler.Input;

import java.lang.reflect.Field;

public class KeywordInputMetadataExtractor implements FieldMetadataExtractor {
    @Override
    public FieldMetadata extractMetadata(Class<?> objectClass, Field field) {
        String parameterName;
        boolean required = false;
        String defaultValue = null;
        if (field.isAnnotationPresent(Input.class)) {
            Input input = field.getAnnotation(Input.class);
            parameterName = input.name() == null || input.name().isEmpty() ? field.getName() : input.name();

            if (input.required()) {
                required = true;
            }

            if (input.defaultValue() != null && !input.defaultValue().isEmpty()) {
                defaultValue = input.defaultValue();
            }
        } else {
            parameterName = field.getName();
        }

        return new FieldMetadata(parameterName, defaultValue, field.getType(), field.getGenericType(), null, required);
    }
}
