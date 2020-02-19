package step.handlers.javahandler;

import java.io.Closeable;
import java.io.IOException;

public class MyKeywordLibrary extends AbstractKeyword {

	@Keyword
	public void MyKeyword() {
		output.add("test", "test");
	}
	
	@Keyword(name="My Keyword")
	public void MyKeywordWithCustomName() {
		output.add("test", "test");
	}
	
	@Keyword
	public void MyErrorKeyword() throws Exception {
		output.setError("My error");
	}
	
	@Keyword
	public void MyExceptionKeyword() throws Exception {
		throw new Exception("My exception");
	}
	
	@Keyword
	public void MyErrorKeywordWithThrowable() throws Throwable {
		throw new Throwable("My throwable");
	}
	
	@Keyword
	public void MyKeywordUsingProperties() {
		echoProperties();
	}
	
	@Keyword(properties = {"prop1"})
	public void MyKeywordWithPropertyAnnotation() {
		echoProperties();
	}
	
	@Keyword(properties = {"prop.{myPlaceHolder}"}, optionalProperties = {"myOptionalProperty"})
	public void MyKeywordWithPlaceHoldersInProperties() {
		echoProperties();
	}
	
	protected void echoProperties() {
		properties.entrySet().forEach(e->{
			output.add(e.getKey(), e.getValue());
		});
	}
	
	@Keyword
	public void MyKeywordUsingSession1() {
		session.put("object1","Test String");
		System.setProperty("testProperty", "test");
		session.put(new Closeable() {
			
			@Override
			public void close() throws IOException {
				System.clearProperty("testProperty");
			}
		});
	}
	
	@Keyword
	public void MyKeywordUsingSession2() {
		output.add("sessionObject", (String)session.get("object1"));
	}
}
