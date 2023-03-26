package step.handlers.javahandler;

public class MyKeywordWithSchemaLibrary extends AbstractKeyword {

	@Keyword(
			name = "My Keyword with custom schema",
			jsonSchema = @JsonSchema(
					properties = {
							@JsonSchemaProperty(name = "numberField", type = "number", defaultV = "1", required = true),
							@JsonSchemaProperty(name = "booleanField", type = "boolean", defaultV = "true", required = true),
							@JsonSchemaProperty(name = "stringField", type = "string", defaultV = "myValue", required = true),
							@JsonSchemaProperty(name = "stringField2", type = "string", defaultV = "myValue2", required = false)
					}))
	public void MyKeywordWithCustomSchema() {
		output.add("test", "test");
	}

	@Keyword(
			name = "My Keyword with custom schema",
			jsonSchema = @JsonSchema(
					properties = {
							@JsonSchemaProperty(name = "stringField", type = "string", defaultV = "myValue", required = true),
							@JsonSchemaProperty(name = "stringField2", type = "string", defaultV = "myValue2", required = false),
							@JsonSchemaProperty(name = "propertyWithNestedFields", type = "object", nestedPropertiesRefs = {"nestedStringProperty1", "nestedNumberProperty1"})
					},
					propertiesRefs = {
							@JsonSchemaPropertyRef(id = "nestedStringProperty1", property = @JsonSchemaProperty(name = "nestedStringProperty", type = "string", defaultV = "nestedValue1")),
							@JsonSchemaPropertyRef(id = "nestedNumberProperty1", property = @JsonSchemaProperty(name = "nestedNumberProperty", type = "number", defaultV = "2"))
					}))
	public void MyKeywordWithCustomSchemaNested() {
		output.add("test", "test");
	}
}
