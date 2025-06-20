package step.handlers.javahandler;

import java.math.BigDecimal;
import java.util.*;

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
										 @Input(name = "stringList", defaultValue = "1;2;3") ArrayList<String> stringList,
										 @Input(name = "booleanList", defaultValue = "true;false;true") ArrayList<Boolean> booleanList) {
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

		if (booleanList != null) {
			output.add("booleanListOut", booleanList.stream().map(Object::toString).reduce((s, s2) -> s + "+" + s2).orElse(""));
		}
	}

	@Keyword
	public void MyKeywordWithInputNumberTypes(@Input(name = "longValue") long longValue,
											  @Input(name = "bigDecimal")BigDecimal bigDecimal) {
		if (! (bigDecimal instanceof BigDecimal)) {
			throw new RuntimeException("Not a big decimal");
		}
		long expected = 111111111111111111L;
		if (expected != longValue) {
			throw new RuntimeException("Incorrect long received as input");
		}
	}

	@Keyword
	public void MyKeywordWithInputMapDefaultValues(@Input(name = "stringMap", defaultValue = "{\"myKey\":\"myValue\",\"myKey2\":\"myValue2\"}") HashMap<String,String> map) {
		map.forEach((k,v) -> output.add(k,v));
	}

	@Keyword
	public void MyKeywordWithCustomMapAndCustomList(@Input(name = "customMap") Map<String, Object> map,
			   @Input(name = "customList") List<Object> myList) {
		assert (String.class.isAssignableFrom(map.get("string").getClass()) && (map.get("string")).equals("myValue"));
		assert (Integer.class.isAssignableFrom(map.get("int").getClass()) && ((Integer) map.get("int")) == 123);
		assert (Long.class.isAssignableFrom(map.get("long").getClass()) && ((Long) map.get("long")) == 1234567891012L);
		assert (Double.class.isAssignableFrom(map.get("double").getClass()) && ((Double) map.get("double")) == 123.4567);
		assert (Boolean.class.isAssignableFrom(map.get("boolean").getClass()) && ((Boolean) map.get("boolean")));
		assert (Map.class.isAssignableFrom(map.get("nestedMap").getClass()) && ((Map) map.get("nestedMap")).get("nestedString").equals("nestedStringValue"));
		assert (String.class.isAssignableFrom(myList.get(0).getClass()) && myList.get(0).equals("val1"));
		assert (Integer.class.isAssignableFrom(myList.get(1).getClass()) && ((Integer) myList.get(1)) == 123);
		assert (Long.class.isAssignableFrom(myList.get(2).getClass()) && ((Long) myList.get(2)) == 1234567891012L);
		assert (Double.class.isAssignableFrom(myList.get(3).getClass()) && ((Double) myList.get(3)) == 123.4567);
		assert (Boolean.class.isAssignableFrom(myList.get(4).getClass()) && ((Boolean) myList.get(4)));
		assert (List.class.isAssignableFrom(myList.get(5).getClass()) && ((List<?>) myList.get(5)).get(0).equals("sublistelement"));
	}

	@Keyword
	public void MyKeywordWithInputMaps(@Input(name = "stringMap", required = true) Map<String,String> stringMap,
									   @Input(name = "stringLinkedHashMap", required = true) LinkedHashMap<String, String> stringLinkedHashMap,
									   @Input(name = "integerMap", required = true) HashMap<String,Integer> integerMap,
									   @Input(name = "mapMapString") HashMap<String, HashMap<String, String>> mapMapString,
									   @Input(name = "arrayOfMaps") List<Map<String,String>> arrayOfMaps) {
		if (!(stringMap instanceof HashMap)) {
			throw new RuntimeException("stringMap is not a Hashmap");
		}
		if (!(stringLinkedHashMap instanceof LinkedHashMap)) {
			throw new RuntimeException("stringLinkedHashMap is not a LinkedHashMap");
		}
		if (!(stringLinkedHashMap.get("myKey") instanceof String)) {
			throw new RuntimeException("stringLinkedHashMap values are not of type String");
		}
		if (!(integerMap.get("myKey") instanceof Integer)) {
			throw new RuntimeException("integerMap values are not of type Integer");
		}
		if (!(mapMapString.get("myKey") instanceof Map)) {
			throw new RuntimeException("mapMapString values are not a map");
		}
		output.add("valueStringHashMap1", stringMap.get("myKey"));
		output.add("valueLinkedHashMap1", stringLinkedHashMap.get("myKey"));
		output.add("valueIntegerHashMap1", integerMap.get("myKey"));
		output.add("valueStringMapMap1", mapMapString.get("myKey").get("mySubKey"));
	//	output.add("arrayOfMapsValue1", arrayOfMaps.get(0).get("myKey"));

	}

	@Keyword
	public void MyKeywordWithInputAnnotationDefaultValues(@Input(name = "numberField", defaultValue = "1") Integer numberField,
											 @Input(name = "primitiveInt", defaultValue = "1") int primitiveInt,
											 @Input(name = "booleanField", defaultValue = "true") Boolean booleanField,
											 @Input(name = "stringField", defaultValue = "myDefaultValue", required = true) String stringField,
											 @Input(name = "stringField2", defaultValue = "myDefaultValue2") String secondStringField,
											 @Input(name = "propertyWithNestedFields") ClassWithNestedFields classWithNestedFields,
											 @Input(name = "stringArray", defaultValue = "a;b;c") String[] stringArray,
											 @Input(name = "integerArray", defaultValue = "1;2;3") Integer[] integerArray,
											 @Input(name = "stringList", defaultValue = "1;2;3") ArrayList<String> stringList) {
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
		output.add("classWithNestedFieldsNotNull", classWithNestedFields == null ? "false" : "true");
		if (classWithNestedFields != null && classWithNestedFields.getNestedStringProperty() != null) {
			output.add("classWithNestedFieldsOut.nestedStringProperty", classWithNestedFields.getNestedStringProperty());
		}
		if (classWithNestedFields != null && classWithNestedFields.getNestedNumberProperty() != null) {
			output.add("classWithNestedFieldsOut.nestedNumberProperty", classWithNestedFields.getNestedNumberProperty());
		}
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

	@Keyword
	public ClassWithNestedFields MyKeywordWithPojoAsInputAndOutput(ClassWithNestedFields input) {
		return input;
	}

	public static class ClassWithNestedFields {
		public String nestedStringProperty;

		public Integer nestedNumberProperty;

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

	private static class MyCustomMap extends HashMap<String, Object> {

		public MyCustomMap() {
			super();
		}
	}

	private static class MyList extends ArrayList<Object> {

		public MyList() {
			super();
		}
	}
}
