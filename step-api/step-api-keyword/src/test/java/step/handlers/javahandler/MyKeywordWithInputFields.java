package step.handlers.javahandler;

import java.util.ArrayList;
import java.util.Arrays;

public class MyKeywordWithInputFields extends AbstractKeyword {

	@Keyword
	public void MyKeywordWithInputAnnotation(@Input(name = "numberField", defaultValue = "1", required = true) Integer numberField,
											 @Input(name = "primitiveInt", defaultValue = "1", required = true) int primitiveInt,
											 @Input(name = "booleanField", defaultValue = "true", required = true) Boolean booleanField,
											 @Input(name = "stringField", defaultValue = "myDefaultValue", required = true) String stringField,
											 @Input(name = "stringField2") String secondStringField) {
		// fill output to check execution result in tests
		if (numberField != null) {
			output.add("numberFieldOut", numberField);
		}
		output.add("primitiveIntOut", primitiveInt);
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

	@Keyword
	public void MyKeywordWithInputArrays(@Input(name = "stringArray", defaultValue = "a;b;c", required = true) String[] stringArray,
										 @Input(name = "integerArray", defaultValue = "1;2;3") Integer[] integerArray,
										 @Input(name = "stringList", defaultValue = "1;2;3") ArrayList<String> stringList) {
		// fill output to check execution result in tests
		if (stringArray != null) {
			output.add("stringArrayOut", Arrays.stream(stringArray).reduce((s, s2) -> s + "+" + s2).orElse(""));
		}

		if (integerArray != null) {
			output.add("integerArrayOut", Arrays.stream(integerArray).map(Object::toString).reduce((integer, integer2) -> integer + "+" + integer2).orElse(""));
		}

		if (stringList != null) {
			output.add("stringListOut", stringList.stream().reduce((s, s2) -> s + "+" + s2).orElse(""));
		}
	}

	public static class ClassWithNestedFields {
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
