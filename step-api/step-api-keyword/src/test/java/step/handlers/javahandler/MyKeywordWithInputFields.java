package step.handlers.javahandler;

public class MyKeywordWithInputFields extends AbstractKeyword {

	@Keyword
	public void MyKeywordWithInputAnnotation(@Input(name = "numberField", defaultValue = "1", required = true) Integer numberField,
											 @Input(name = "booleanField", defaultValue = "true", required = true) Boolean booleanField,
											 @Input(name = "stringField", defaultValue = "myValue", required = true) String stringField,
											 @Input(name = "stringField2", defaultValue = "myValue2") String secondStringField) {
		output.add("test", "test");
	}

	@Keyword
	public void MyKeywordWithCustomSchemaNested( @Input(name = "stringField", defaultValue = "myValue", required = true) String stringField,
												 @Input(name = "stringField2", defaultValue = "myValue2") String secondStringField,
												 @Input(name = "propertyWithNestedFields") ClassWithNestedFields classWithNestedFields) {
		output.add("test", "test");
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
