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

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class MyKeywordLibrary extends AbstractKeyword {

	public static final String ON_ERROR_MARKER = "onError";
	public static final String THROW_EXCEPTION_IN_AFTER = "throwExceptionInAfter";
	public static final String THROW_EXCEPTION_IN_BEFORE = "throwExceptionInBefore";
	public static final String RETHROW_EXCEPTION_IN_ON_ERROR = "rethrowException";

	private String keywordClassProperty;

	public String getKeywordClassProperty() {
		return keywordClassProperty;
	}

	public void setKeywordClassProperty(String keywordClassProperty) {
		this.keywordClassProperty = keywordClassProperty;
	}

	@Override
	public void beforeKeyword(String keywordName,Keyword keyword) {
		if (!keywordName.contains("Return")) {
			output.add("beforeKeyword", keywordName);
		}
		if(getBooleanProperty(THROW_EXCEPTION_IN_BEFORE)) {
			throw new RuntimeException(THROW_EXCEPTION_IN_BEFORE);
		}

	}

	@Override
	public void afterKeyword(String keywordName,Keyword keyword) {
		output.add("afterKeyword",keywordName);
		if(getBooleanProperty(THROW_EXCEPTION_IN_AFTER)) {
			throw new RuntimeException(THROW_EXCEPTION_IN_AFTER);
		}
	}

	@Override
	public boolean onError(Exception e) {
		Throwable cause = e.getCause();
		output.add(ON_ERROR_MARKER, cause != null ? cause.getMessage() : e.getMessage());
		return getBooleanProperty(RETHROW_EXCEPTION_IN_ON_ERROR, true);
	}

	private boolean getBooleanProperty(String key) {
		return Boolean.parseBoolean(properties.getOrDefault(key, Boolean.FALSE.toString()));
	}

	private boolean getBooleanProperty(String key, boolean defaultValue) {
		return Boolean.parseBoolean(properties.getOrDefault(key, Boolean.toString(defaultValue)));
	}

	@Keyword
	public void MyKeyword() {
		output.add("test", "test");
	}

	@Keyword
	public Map<String, Serializable> MyKeywordWithReturn() {
		assert this.properties.get("myProperty").equals("myPropertyValue");
//		return output.build().getPayload(); --> won't work as not serializable
		return Map.of("key", "someStringValue","long", 12345798798L, "double", 123.456);
	}

	@Keyword
	public MyPojo MyKeywordWithReturnPojo() {
		assert this.properties.get("myProperty").equals("myPropertyValue");
		return new MyPojo();
	}

	@Keyword
	public PojoWithPrivateFields MyKeywordReturningPojoWithPrivateFields() {
		PojoWithPrivateFields pojoWithPrivateFields = new PojoWithPrivateFields();
		pojoWithPrivateFields.setStringField("some value");
		return pojoWithPrivateFields;
	}

	@Keyword
	public PojoWithPrivateFields MyKeywordWithReturnPojoAndOutputUsage() {
		output.add("test", "test");
		PojoWithPrivateFields pojoWithPrivateFields = new PojoWithPrivateFields();
		pojoWithPrivateFields.setStringField("some value");
		return pojoWithPrivateFields;
	}

	public static class MyPojo {
		public String stringField = "someValue";
		public long longValue = 123456787L;
		public boolean booleanValue = true;
		public double aoubleValue = 123.4567;
		public List<Object> someList = List.of("text", true, 12345678L);
		public Map<String, Object> someMap = Map.of("string", "value", "long", 123456748L, "boolean", true);
	}

	public static class PojoWithPrivateFields {
		private String stringField ;

		public String getStringField() {
			return stringField;
		}

		public void setStringField(String stringField) {
			this.stringField = stringField;
		}
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

	@Keyword(routing = {})
	public void MyKeywordWithDefaultRouting() {	}

	@Keyword(routing = {Keyword.ROUTING_EXECUTE_ON_CONTROLLER})
	public void MyKeywordWithRoutingToController() {	}

	@Keyword(routing = {"OS", "Windows","type","playwright"})
	public void MyKeywordWithRoutingToAgentsWithCriteria() {	}
	
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
	public void MyKeywordUsingSessionWithAutocloseable() {
		session.put("object1","Test String");
		System.setProperty("testProperty2", "test2");
		session.put(new AutoCloseable() {

			@Override
			public void close() throws IOException {
				System.clearProperty("testProperty2");
			}
		});
	}
	
	@Keyword
	public void MyKeywordUsingSession2() {
		output.add("sessionObject", (String)session.get("object1"));
	}

}
