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
package step.handlers.javahandler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class KeywordRunnerTest {

	private static final Logger log = LoggerFactory.getLogger(KeywordRunnerTest.class);

	@Test
	public void test() throws Exception {
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("MyKeyword");
		Assert.assertEquals("test",output.getPayload().getString("test"));
		Assert.assertEquals("MyKeyword",output.getPayload().getString("beforeKeyword"));
		Assert.assertEquals("MyKeyword",output.getPayload().getString("afterKeyword"));
	}
	
	@Test
	public void testCustomKeywordName() throws Exception {
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("My Keyword");
		Assert.assertEquals("test",output.getPayload().getString("test"));
		Assert.assertEquals("My Keyword",output.getPayload().getString("beforeKeyword"));
		Assert.assertEquals("My Keyword",output.getPayload().getString("afterKeyword"));
	}
	
	@Test
	public void testPropertiesWithoutValidation() throws Exception {
		Map<String, String> properties = new HashMap<>();
		String myPropertyKey = "prop1";
		String myPropertyValue = "My Property";
		properties.put(myPropertyKey, myPropertyValue);
		ExecutionContext runner = KeywordRunner.getExecutionContext(properties, MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("MyKeywordUsingProperties");
		Assert.assertEquals(myPropertyValue,output.getPayload().getString(myPropertyKey));
	}
	
	@Test
	public void testPropertyValidation() throws Exception {
		Map<String, String> properties = new HashMap<>();
		String myPropertyValue = "My Property";
		String myPropertyKey = "prop1";
		properties.put(myPropertyKey, myPropertyValue);
		properties.put("prop2", "My Property 2");
		properties.put(KeywordExecutor.VALIDATE_PROPERTIES, "true");
		ExecutionContext runner = KeywordRunner.getExecutionContext(properties, MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("MyKeywordWithPropertyAnnotation");
		Assert.assertEquals(myPropertyValue,output.getPayload().getString(myPropertyKey));
		Assert.assertEquals(3, output.getPayload().keySet().size());
	}
	
	@Test
	public void testPropertyValidationPropertyMissing() throws Exception {
		Map<String, String> properties = new HashMap<>();
		properties.put("prop2", "My Property 2");
		properties.put(KeywordExecutor.VALIDATE_PROPERTIES, "My Property 2");
		ExecutionContext runner = KeywordRunner.getExecutionContext(properties, MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("MyKeywordWithPropertyAnnotation");
		Assert.assertEquals("The Keyword is missing the following properties [prop1]",output.getError().getMsg());
	}
	
	@Test
	public void testPropertyValidationWithPlaceHolder() throws Exception {
		Map<String, String> properties = new HashMap<>();
		properties.put("myPlaceHolder", "placeHolderValue");
		String myPropertyValue = "My Property with Place holder";
		String myPropertyKey = "prop.placeHolderValue";
		properties.put(myPropertyKey, myPropertyValue);
		
		properties.put(KeywordExecutor.VALIDATE_PROPERTIES, "true");
		ExecutionContext runner = KeywordRunner.getExecutionContext(properties, MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("MyKeywordWithPlaceHoldersInProperties");
		Assert.assertEquals(myPropertyValue,output.getPayload().getString(myPropertyKey));
		Assert.assertEquals(3, output.getPayload().keySet().size());
	}
	
	@Test
	public void testPropertyValidationWithPlaceHolderInInput() throws Exception {
		Map<String, String> properties = new HashMap<>();
		String myPropertyValue = "My Property with Place holder";
		String myPropertyKey = "prop.placeHolderValue";
		properties.put(myPropertyKey, myPropertyValue);
		// The placeholder value from the input should be taken
		properties.put("myPlaceHolder", "placeHolderValueFromProperties");
		
		properties.put(KeywordExecutor.VALIDATE_PROPERTIES, "validate");
		ExecutionContext runner = KeywordRunner.getExecutionContext(properties, MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("MyKeywordWithPlaceHoldersInProperties","{\"myPlaceHolder\": \"placeHolderValue\"}");
		Assert.assertNull(output.getError());
		Assert.assertEquals(myPropertyValue,output.getPayload().getString(myPropertyKey));
		Assert.assertEquals(3, output.getPayload().keySet().size());
	}
	
	@Test
	public void testPropertyValidationWithOptionalProperties() throws Exception {
		Map<String, String> properties = new HashMap<>();
		String myPropValue = "My Property with Place holder";
		String myPropKey = "prop.placeHolderValue";
		properties.put(myPropKey, myPropValue);
		String myOptionalPropertyKey = "myOptionalProperty";
		String myOptionalPropertyValue = "My optional Property";
		properties.put(myOptionalPropertyKey, myOptionalPropertyValue);
		
		properties.put(KeywordExecutor.VALIDATE_PROPERTIES, "validate");
		ExecutionContext runner = KeywordRunner.getExecutionContext(properties, MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("MyKeywordWithPlaceHoldersInProperties","{\"myPlaceHolder\": \"placeHolderValue\"}");
		Assert.assertNull(output.getError());
		Assert.assertEquals(myPropValue,output.getPayload().getString(myPropKey));
		Assert.assertEquals(myOptionalPropertyValue,output.getPayload().getString(myOptionalPropertyKey));
		Assert.assertEquals(4, output.getPayload().keySet().size());
	}
	
	@Test
	public void testPropertyValidationWithPlaceHolderInInputWhereTheResolvedPropertyIsMissing() throws Exception {
		Map<String, String> properties = new HashMap<>();
		properties.put("other.placeHolderValue", "My Property with Place holder");
		
		properties.put(KeywordExecutor.VALIDATE_PROPERTIES, "validate");
		ExecutionContext runner = KeywordRunner.getExecutionContext(properties, MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("MyKeywordWithPlaceHoldersInProperties","{\"myPlaceHolder\": \"placeHolderValue\"}");
		Assert.assertEquals("The Keyword is missing the following properties [prop.placeHolderValue]", output.getError().getMsg());
	}
	
	@Test
	public void testPropertyValidationWithPlaceHolderInInputWhereThePlaceholderIsMissing() throws Exception {
		Map<String, String> properties = new HashMap<>();
		properties.put("prop.placeHolderValue", "My Property with Place holder");
		
		properties.put(KeywordExecutor.VALIDATE_PROPERTIES, "validate");
		ExecutionContext runner = KeywordRunner.getExecutionContext(properties, MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("MyKeywordWithPlaceHoldersInProperties","{}");
		Assert.assertEquals("The Keyword is missing the following property or input 'myPlaceHolder'", output.getError().getMsg());
	}
	
	@Test
	public void testSession() throws Exception {
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("MyKeywordUsingSession1");
		Assert.assertEquals("test", System.getProperty("testProperty"));
		output = runner.run("MyKeywordUsingSession2");
		Assert.assertEquals("Test String",output.getPayload().getString("sessionObject"));
		runner.close();
		// Asserts that the close method of the session object created in MyKeywordUsingSession2 has been called
		Assert.assertNull(System.getProperty("testProperty"));

		output = runner.run("MyKeywordUsingSessionWithAutocloseable");
		Assert.assertEquals("test2", System.getProperty("testProperty2"));
		output = runner.run("MyKeywordUsingSession2");
		Assert.assertEquals("Test String",output.getPayload().getString("sessionObject"));
		runner.close();
		// Asserts that the close method of the session object created in MyKeywordUsingSession2 has been called
		Assert.assertNull(System.getProperty("testProperty2"));
	}
	
	@Test
	public void testError() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		try {
			runner.run("MyErrorKeyword");
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertEquals("My error",exception.getMessage());
	}

	@Test
	public void testExceptionsInBeforeHook() throws Exception {
		Map<String, String> properties = new HashMap<>();
		properties.put(MyKeywordLibrary.THROW_EXCEPTION_IN_BEFORE, Boolean.TRUE.toString());
		ExecutionContext runner = KeywordRunner.getExecutionContext(properties, MyKeywordLibrary.class);
		runner.setThrowExceptionOnError(false);
		Output<JsonObject> output = runner.run("MyKeyword");
		// Assert that the onError hook has been called and that the output set within it are available
		Assert.assertEquals(MyKeywordLibrary.THROW_EXCEPTION_IN_BEFORE, output.getPayload().getString(MyKeywordLibrary.ON_ERROR_MARKER));
		// Assert that the keyword hasn't been called
		Assert.assertFalse(output.getPayload().containsKey("test"));
		// Assert that the afterKeyword hook has been called
		Assert.assertEquals("MyKeyword",output.getPayload().getString("afterKeyword"));
	}

	@Test
	public void testOnErrorHook() throws Exception {
		Map<String, String> properties = new HashMap<>();
		// Enable error rethrow
		properties.put(MyKeywordLibrary.RETHROW_EXCEPTION_IN_ON_ERROR, Boolean.TRUE.toString());
		ExecutionContext runner = KeywordRunner.getExecutionContext(properties, MyKeywordLibrary.class);
		runner.setThrowExceptionOnError(false);
		Output<JsonObject> output = runner.run("MyExceptionKeyword");
		// Assert that the error has been handled properly
		Assert.assertEquals("My exception", output.getError().getMsg());
		Assert.assertEquals("Error{type=TECHNICAL, layer='keyword', msg='My exception', code=0, root=true}", output.getError().toString());
		// Assert that the onError hook has been called and that the output set within it are available
		Assert.assertEquals("My exception", output.getPayload().getString(MyKeywordLibrary.ON_ERROR_MARKER));
		// Assert that the afterKeyword hook has been called
		Assert.assertEquals("MyExceptionKeyword",output.getPayload().getString("afterKeyword"));
	}

	@Test
	public void testOnErrorHookWithoutRethrow() throws Exception {
		Map<String, String> properties = new HashMap<>();
		// Disable exception rethrow after the onError hook
		properties.put(MyKeywordLibrary.RETHROW_EXCEPTION_IN_ON_ERROR, Boolean.FALSE.toString());
		ExecutionContext runner = KeywordRunner.getExecutionContext(properties, MyKeywordLibrary.class);
		runner.setThrowExceptionOnError(false);
		Output<JsonObject> output = runner.run("MyExceptionKeyword");
		// Assert that the error isn't set as the exception rethrow has been disabled
		Assert.assertNull(output.getError());
		// Assert that the onError hook has been called and that the output set within it are available
		Assert.assertEquals("My exception", output.getPayload().getString(MyKeywordLibrary.ON_ERROR_MARKER));
		// Assert that the afterKeyword hook has been called
		Assert.assertEquals("MyExceptionKeyword",output.getPayload().getString("afterKeyword"));
	}
	
	@Test
	public void testException() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		try {
			runner.run("MyExceptionKeyword");
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertEquals("My exception",exception.getMessage());
		Assert.assertTrue(exception instanceof KeywordException);
		// the exception thrown by the keyword is attached as cause
		Assert.assertNotNull(exception.getCause());
	}

	@Test
	public void testErrorKeywordWithThrowable() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		try {
			runner.run("MyErrorKeywordWithThrowable");
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertEquals("My throwable",exception.getMessage());
	}
	
	@Test
	public void testRunnerDoesntThrowExceptionOnError() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		// we're testing here the following flag. In that case no exception should be thrown in case of an error
		runner.setThrowExceptionOnError(false);
		
		Output<JsonObject> output = null;
		try {
			output = runner.run("MyErrorKeyword");
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertNotNull(output);
		Assert.assertEquals("My error",output.getError().getMsg());
		Assert.assertNull(exception);
	}
	
	@Test
	public void testRunnerDoesntThrowExceptionOnException() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		// we're testing here the following flag. In that case exceptions thrown in the keyword should be reported as error
		runner.setThrowExceptionOnError(false);
		
		Output<JsonObject> output = null;
		try {
			output = runner.run("MyExceptionKeyword");
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertNotNull(output);
		Assert.assertEquals("My exception",output.getError().getMsg());
		Assert.assertNull(exception);
	}
	
	@Test
	public void testKeywordNotExisting() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		try {
			runner.run("UnexistingKeyword");
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertEquals("Unable to find method annotated by 'step.handlers.javahandler.Keyword' with name=='UnexistingKeyword'",exception.getMessage());
	}
	
	@Test
	public void testEmptyLibraryList() throws Exception {
		Exception exception = null;
		try {
			KeywordRunner.getExecutionContext();
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertEquals("Please specify at least one class containing the keyword definitions",exception.getMessage());
	}
	
	@Test
	public void testEmptyKeywordLibrary() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyEmptyKeywordLibrary.class);
		try {
			runner.run("MyKeyword");
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertEquals("Unable to find method annotated by 'step.handlers.javahandler.Keyword' with name=='MyKeyword'",exception.getMessage());
	}
	
	@Test
	public void testKeywordLibraryThatDoesntExtendAbstractKeyword() throws Exception {
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibraryThatDoesntExtendAbstractKeyword.class);
		Output<JsonObject> output = runner.run("MyKeyword");
		Assert.assertEquals("The class '"+MyKeywordLibraryThatDoesntExtendAbstractKeyword.class.getName()+"' doesn't extend '"+AbstractKeyword.class.getName()+"'. Extend this class to get input parameters from STEP and return output.", output.getPayload().getString("Info"));
	}

	@Test
	public void testKeywordWithSimpleAttributes() throws Exception {
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordWithInputFields.class);

		Output<JsonObject> output = runner.run(
				"MyKeywordWithInputAnnotation",
				readJsonFromFile("src/test/resources/step/handlers/javahandler/simple-json-input-1.json").toString()
		);

		JsonObject result = output.getPayload();
		log.info("Execution result: {}", result.toString());

		Assert.assertEquals(77, result.getInt("numberFieldOut"));
		Assert.assertEquals(88, result.getInt("primitiveIntOut"));
		Assert.assertTrue(result.getBoolean("booleanFieldOut"));
		Assert.assertEquals("myValue1", result.getString("stringField1Out"));
		Assert.assertEquals("myValue2", result.getString("stringField2Out"));
	}

	@Test
	public void testKeywordWithNestedAttributes() throws Exception {
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordWithInputFields.class);

		Output<JsonObject> output = runner.run(
				"MyKeywordWithInputNested",
				readJsonFromFile("src/test/resources/step/handlers/javahandler/nested-json-input-1.json").toString()
		);

		JsonObject result = output.getPayload();
		log.info("Execution result: {}", result.toString());

		Assert.assertEquals("myValue1", result.getString("stringField1Out"));
		Assert.assertEquals("myValue2", result.getString("stringField2Out"));
		Assert.assertEquals("true", result.getString("classWithNestedFieldsNotNull"));
		Assert.assertEquals(77, result.getInt("classWithNestedFieldsOut.nestedNumberProperty"));
		Assert.assertEquals("myValue3", result.getString("classWithNestedFieldsOut.nestedStringProperty"));
	}

	@Test
	public void testKeywordWithNullAttributes() throws Exception {
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordWithInputFields.class);

		Output<JsonObject> output = runner.run(
				"MyKeywordWithInputAnnotation",
				readJsonFromFile("src/test/resources/step/handlers/javahandler/null-json-input-1.json").toString()
		);

		JsonObject result = output.getPayload();
		log.info("Execution result: {}", result.toString());

		Assert.assertFalse(result.containsKey("numberFieldOut"));
		Assert.assertFalse(result.containsKey("booleanFieldOut"));
		Assert.assertFalse(result.containsKey("stringField1Out"));
		Assert.assertFalse(result.containsKey("stringField2Out"));
	}

	@Test
	public void testKeywordWithArrays() throws Exception {
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordWithInputFields.class);

		Output<JsonObject> output = runner.run(
				"MyKeywordWithInputArrays",
				readJsonFromFile("src/test/resources/step/handlers/javahandler/array-json-input-1.json").toString()
		);

		JsonObject result = output.getPayload();
		log.info("Execution result: {}", result.toString());

		Assert.assertEquals("d+e+f", result.getString("stringArrayOut"));
		Assert.assertEquals("4+5+6", result.getString("integerArrayOut"));
		Assert.assertEquals("d+e+f", result.getString("stringListOut"));
	}

	private static JsonNode readJsonFromFile(String path) throws IOException {
		File inputFile = new File(path);

		JsonFactory factory = new JsonFactory();
		ObjectMapper mapper = new ObjectMapper(factory);

		return mapper.readTree(inputFile);
	}
}
