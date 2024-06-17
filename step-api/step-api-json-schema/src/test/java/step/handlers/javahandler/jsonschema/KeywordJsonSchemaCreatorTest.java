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
import java.util.*;
import java.util.function.Supplier;

public class KeywordJsonSchemaCreatorTest {

	public static final Logger log = LoggerFactory.getLogger(KeywordJsonSchemaCreatorTest.class);

	@Test
	public void jsonInputParamsReaderTest() throws Throwable {
		step.handlers.javahandler.jsonschema.KeywordJsonSchemaCreator reader = new step.handlers.javahandler.jsonschema.KeywordJsonSchemaCreator();

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
		step.handlers.javahandler.jsonschema.KeywordJsonSchemaCreator reader = new step.handlers.javahandler.jsonschema.KeywordJsonSchemaCreator();

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
		step.handlers.javahandler.jsonschema.KeywordJsonSchemaCreator reader = new KeywordJsonSchemaCreator();

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

	@Test
	public void jsonInputParamsReaderMapFieldsTest() throws Throwable {
		step.handlers.javahandler.jsonschema.KeywordJsonSchemaCreator reader = new KeywordJsonSchemaCreator();

		Method method = Arrays.stream(KeywordTestClass.class.getMethods()).filter(m -> m.getName().equals("MyKeywordWithInputMaps")).findFirst().orElseThrow((Supplier<Throwable>) () -> new RuntimeException("Test class not found"));

		log.info("Check json schema for method " + method.getName());
		JsonObject schema = reader.createJsonSchemaForKeyword(method);
		String jsonString = schema.toString();
		log.info(jsonString);

		File expectedSchema = new File("src/test/resources/step/handlers/javahandler/jsonschema/expected-maps-json-input-schema.json");

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
												 @Input(name = "intPrimitiveField", defaultValue = "2", required = true) int intPrimitive,
												 @Input(name = "booleanPrimitiveField", defaultValue = "true", required = true) boolean booleanPrimitive,
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
											 @Input(name = "integerArray", defaultValue = "1;2;3") Integer[] integerArray,
											 @Input(name = "stringList", defaultValue = "c;d;e") List<String> stringList) {
			output.add("test", "test");
		}

		@Keyword
		public void MyKeywordWithInputMaps(@Input(name = "stringMap", required = true) HashMap<String,String> stringMap,
										   @Input(name = "stringLinkedHashMap", required = true) LinkedHashMap<String, String> stringLinkedHashMap,
											 @Input(name = "integerMap") HashMap<String,Integer> integerMap,
											 @Input(name = "mapMapString") HashMap<String, HashMap<String, String>> mapMapString) {
			output.add("test", "test");
		}
	}

	public static class ClassWithNestedFields extends TestParent {
		@Input(defaultValue = "nestedValue1", required = true)
		private String nestedString;

		private List<Integer> nestedNumberList;

		public ClassWithNestedFields() {
		}

		public String getNestedString() {
			return nestedString;
		}

		public void setNestedString(String nestedString) {
			this.nestedString = nestedString;
		}

		public List<Integer> getNestedNumberList() {
			return nestedNumberList;
		}

		public void setNestedNumberList(List<Integer> nestedNumberList) {
			this.nestedNumberList = nestedNumberList;
		}
	}

	private static class TestParent {
		private String parentProperty;

		public String getParentProperty() {
			return parentProperty;
		}

		public void setParentProperty(String parentProperty) {
			this.parentProperty = parentProperty;
		}
	}
}