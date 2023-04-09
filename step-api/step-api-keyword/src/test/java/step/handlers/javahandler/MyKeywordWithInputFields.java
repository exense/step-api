package step.handlers.javahandler;

public class MyKeywordWithInputFields extends AbstractKeyword {

	@Keyword
	public void MyKeywordWithInputAnnotation(@Input(name = "numberField", defaultValue = "1", required = true) Integer numberField,
											 @Input(name = "booleanField", defaultValue = "true", required = true) Boolean booleanField,
											 @Input(name = "stringField", defaultValue = "myDefaultValue", required = true) String stringField,
											 @Input(name = "stringField2") String secondStringField) {
		// fill output to check execution result in tests
		if (numberField != null) {
			output.add("numberFieldOut", numberField);
		}
		if (booleanField != null) {
			output.add("booleanFieldOut", booleanField);
		}
		if (stringField != null) {
			output.add("stringField1Out", stringField);
		}
		if (secondStringField != null) {
			output.add("stringField2Out", secondStringField);
		}
	}

	@Keyword
	public void MyKeywordWithInputNested(@Input(name = "stringField", defaultValue = "myValue", required = true) String stringField,
										 @Input(name = "stringField2", defaultValue = "myValue2") String secondStringField,
										 @Input(name = "propertyWithNestedFields") ClassWithNestedFields classWithNestedFields) {
		// fill output to check execution result in tests
		if (stringField != null) {
			output.add("stringField1Out", stringField);
		}
		if (secondStringField != null) {
			output.add("stringField2Out", secondStringField);
		}
		output.add("classWithNestedFieldsNotNull", classWithNestedFields == null ? "false" : "true");
		if (classWithNestedFields != null && classWithNestedFields.getNestedStringProperty() != null) {
			output.add("classWithNestedFieldsOut.nestedStringProperty", classWithNestedFields.getNestedStringProperty());
		}
		if (classWithNestedFields != null && classWithNestedFields.getNestedNumberProperty() != null) {
			output.add("classWithNestedFieldsOut.nestedNumberProperty", classWithNestedFields.getNestedNumberProperty());
		}
	}

	public static class ClassWithNestedFields {
		@Input(defaultValue = "nestedValue1")
		private String nestedStringProperty;

		@Input(defaultValue = "2")
		private Integer nestedNumberProperty;

		public ClassWithNestedFields() {
		}

		public String getNestedStringProperty() {
			return nestedStringProperty;
		}

		public void setNestedStringProperty(String nestedStringProperty) {
			this.nestedStringProperty = nestedStringProperty;
		}

		public Integer getNestedNumberProperty() {
			return nestedNumberProperty;
		}

		public void setNestedNumberProperty(Integer nestedNumberProperty) {
			this.nestedNumberProperty = nestedNumberProperty;
		}
	}
}
