package step.handlers.javahandler;

import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class KeywordRunnerTest {

	@Test
	public void test() throws Exception {
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("MyKeyword");
		Assert.assertEquals("test",output.getPayload().getString("test"));
	}
	
	@Test
	public void testCustomKeywordName() throws Exception {
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("My Keyword");
		Assert.assertEquals("test",output.getPayload().getString("test"));
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
		Assert.assertEquals(1, output.getPayload().keySet().size());
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
		Assert.assertEquals(1, output.getPayload().keySet().size());
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
		Assert.assertEquals(1, output.getPayload().keySet().size());
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
		Assert.assertEquals(2, output.getPayload().keySet().size());
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
		Assert.assertEquals("Unable to find method annoted by 'step.handlers.javahandler.Keyword' with name=='UnexistingKeyword'",exception.getMessage());
	}
	
	@Test
	public void testEmptyLibraryList() throws Exception {
		Exception exception = null;
		try {
			KeywordRunner.getExecutionContext();
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertEquals("Please specify at leat one class containing the keyword definitions",exception.getMessage());
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
		Assert.assertEquals("Unable to find method annoted by 'step.handlers.javahandler.Keyword' with name=='MyKeyword'",exception.getMessage());
	}
	
	@Test
	public void testKeywordLibraryThatDoesntExtendAbstractKeyword() throws Exception {
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibraryThatDoesntExtendAbstractKeyword.class);
		Output<JsonObject> output = runner.run("MyKeyword");
		Assert.assertEquals("The class '"+MyKeywordLibraryThatDoesntExtendAbstractKeyword.class.getName()+"' doesn't extend '"+AbstractKeyword.class.getName()+"'. Extend this class to get input parameters from STEP and return output.", output.getPayload().getString("Info"));
	}
}
