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

import java.lang.reflect.Type;

public class FieldMetadata {
    private String fieldName;
    private String defaultValue;
    private Class<?> type;
    private Type genericType;
    private String subSchemaReference;
    private boolean required = false;

    public FieldMetadata(String fieldName, String defaultValue, Class<?> type, boolean required) {
        this.fieldName = fieldName;
        this.defaultValue = defaultValue;
        this.type = type;
        this.required = required;
    }

    public FieldMetadata(String fieldName, String defaultValue, Class<?> type, Type genericType, String subSchemaReference, boolean required) {
        this.fieldName = fieldName;
        this.defaultValue = defaultValue;
        this.type = type;
        this.subSchemaReference = subSchemaReference;
        this.required = required;
        this.genericType = genericType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Class<?> getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Type getGenericType() {
        return genericType;
    }

    public void setGenericType(Type genericType) {
        this.genericType = genericType;
    }

    public String getSubSchemaReference() {
        return subSchemaReference;
    }

    public void setSubSchemaReference(String subSchemaReference) {
        this.subSchemaReference = subSchemaReference;
    }
}
