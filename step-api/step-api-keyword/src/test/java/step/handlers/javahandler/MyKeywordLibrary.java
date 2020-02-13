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
		output.add("prop", properties.get("prop1"));
	}
	
	@Keyword(properties = {"prop1"})
	public void MyKeywordWithPropertyAnnotation() {
		output.add("prop", properties.get("prop1"));
		if(properties.size()!=1) {
			output.setError("The property map contains more than one property. Required was only one property.");
		}
	}
	
	@Keyword(properties = {"prop.{myPlaceHolder}", "myPlaceHolder"})
	public void MyKeywordWithPlaceHoldersInProperty() {
		String fullPropname = "prop." + properties.get("myPlaceHolder");
		output.add(fullPropname, properties.get(fullPropname));
		if(properties.size()!=2) {
			output.setError("The property map contains more than one property. Required was only one property.");
		}
	}
	
	@Keyword(properties = {"prop.{myPlaceHolder}"})
	public void MyKeywordWithPlaceHoldersInInput() {
		String fullPropname = "prop." + input.getString("myPlaceHolder");
		output.add(fullPropname, properties.get(fullPropname));
		if(properties.size()!=1) {
			output.setError("The property map contains more than one property. Required was only one property.");
		}
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
