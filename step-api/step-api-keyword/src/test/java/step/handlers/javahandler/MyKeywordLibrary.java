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
