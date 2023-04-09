package step.handlers.javahandler.jsonschema;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Input;
import step.handlers.javahandler.Keyword;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Supplier;

public class KeywordJsonSchemaCreatorTest {

	public static final Logger log = LoggerFactory.getLogger(KeywordJsonSchemaCreatorTest.class);

	@Test
	public void jsonInputParamsReaderTest() throws Throwable {
		KeywordJsonSchemaCreator reader = new KeywordJsonSchemaCreator();

		Method method = Arrays.stream(KeywordTestClass.class.getMethods()).filter(m -> m.getName().equals("MyKeywordWithInputAnnotation")).findFirst().orElseThrow((Supplier<Throwable>) () -> new RuntimeException("Test class not found"));

		log.info("Check json schema for method " + method.getName());
		JsonObject schema = reader.createJsonSchemaForKeyword(method);
		String jsonString = schema.toString();
		log.info(jsonString);

		File expectedSchema = new File("src/test/resources/step/handlers/javahandler/jsonschema/expected-json-schema-1.json");

		JsonFactory factory = new JsonFactory();
		ObjectMapper mapper = new ObjectMapper(factory);

		// compare json nodes to avoid unstable comparisons in case of changed whitespaces or fields ordering
		JsonNode expectedJsonNode = mapper.readTree(expectedSchema);
		JsonNode actualJsonNode = mapper.readTree(jsonString);

		Assert.assertEquals(expectedJsonNode, actualJsonNode);
	}

	@Test
	public void jsonInputParamsReaderNestedFieldsTest() throws Throwable {
		KeywordJsonSchemaCreator reader = new KeywordJsonSchemaCreator();

		Method method = Arrays.stream(KeywordTestClass.class.getMethods()).filter(m -> m.getName().equals("MyKeywordWithInputNestedFieldAnnotation")).findFirst().orElseThrow((Supplier<Throwable>) () -> new RuntimeException("Test class not found"));

		log.info("Check json schema for method " + method.getName());
		JsonObject schema = reader.createJsonSchemaForKeyword(method);
		String jsonString = schema.toString();
		log.info(jsonString);

		File expectedSchema = new File("src/test/resources/step/handlers/javahandler/jsonschema/expected-json-schema-2.json");

		JsonFactory factory = new JsonFactory();
		ObjectMapper mapper = new ObjectMapper(factory);

		// compare json nodes to avoid unstable comparisons in case of changed whitespaces or fields ordering
		JsonNode expectedJsonNode = mapper.readTree(expectedSchema);
		JsonNode actualJsonNode = mapper.readTree(jsonString);

		Assert.assertEquals(expectedJsonNode, actualJsonNode);
	}

	@Test
	public void jsonInputParamsReaderArrayFieldsTest() throws Throwable {
		KeywordJsonSchemaCreator reader = new KeywordJsonSchemaCreator();

		Method method = Arrays.stream(KeywordTestClass.class.getMethods()).filter(m -> m.getName().equals("MyKeywordWithInputArrays")).findFirst().orElseThrow((Supplier<Throwable>) () -> new RuntimeException("Test class not found"));

		log.info("Check json schema for method " + method.getName());
		JsonObject schema = reader.createJsonSchemaForKeyword(method);
		String jsonString = schema.toString();
		log.info(jsonString);

		File expectedSchema = new File("src/test/resources/step/handlers/javahandler/jsonschema/expected-json-schema-3.json");

		JsonFactory factory = new JsonFactory();
		ObjectMapper mapper = new ObjectMapper(factory);

		// compare json nodes to avoid unstable comparisons in case of changed whitespaces or fields ordering
		JsonNode expectedJsonNode = mapper.readTree(expectedSchema);
		JsonNode actualJsonNode = mapper.readTree(jsonString);

		Assert.assertEquals(expectedJsonNode, actualJsonNode);
	}


	public static class KeywordTestClass extends AbstractKeyword {
		@Keyword
		public void MyKeywordWithInputAnnotation(@Input(name = "numberField", defaultValue = "1", required = true) Integer numberField,
												 @Input(name = "booleanField", defaultValue = "true", required = true) Boolean booleanField,
												 @Input(name = "stringField", defaultValue = "myValue", required = true) String stringField,
												 @Input(name = "stringField2", defaultValue = "myValue2") String secondStringField) {
			output.add("test", "test");
		}

		@Keyword
		public void MyKeywordWithInputNestedFieldAnnotation(@Input(name = "stringField", defaultValue = "myValue", required = true) String stringField,
															@Input(name = "stringField2", defaultValue = "myValue2") String secondStringField,
															@Input(name = "propertyWithNestedFields") ClassWithNestedFields classWithNestedFields) {
			output.add("test", "test");
		}

		@Keyword
		public void MyKeywordWithInputArrays(@Input(name = "stringArray", defaultValue = "a;b;c", required = true) String[] stringArray,
											 @Input(name = "integerArray", defaultValue = "1;2;3") Integer[] integerArray) {
			output.add("test", "test");
		}
	}

	public static class ClassWithNestedFields {
		@Input(defaultValue = "nestedValue1", required = true)
		private String nestedStringProperty;

		@Input(name="numberProperty", defaultValue = "2")
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