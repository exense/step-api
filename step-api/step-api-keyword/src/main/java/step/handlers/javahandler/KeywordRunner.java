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

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.functions.io.AbstractSession;
import step.functions.io.Input;
import step.functions.io.Output;

public class KeywordRunner {
	
	private static final Logger logger = LoggerFactory.getLogger(KeywordRunner.class);

	public static class ExecutionContext {
		
		protected List<Class<?>> functionClasses;
		
		private Map<String, String> contextProperties;
		private AbstractSession tokenSession;
		private AbstractSession tokenReservationSession;
		private KeywordExecutor keywordExecutor;
		
		public ExecutionContext(List<Class<?>> functionClasses, Map<String, String> properties, boolean throwExceptionOnError) {
			super();
			this.functionClasses = functionClasses;
			
			tokenSession = new AbstractSession();
			tokenReservationSession = new AbstractSession();
			contextProperties = properties;
			keywordExecutor = new KeywordExecutor(throwExceptionOnError);
		} 
		
		public void setThrowExceptionOnError(boolean throwExceptionOnError) {
			keywordExecutor.setThrowExceptionOnError(throwExceptionOnError);
		}

		public Output<JsonObject> run(String function, String argument, Map<String, String> properties) throws Exception {
			return run(function, read(argument), properties);
		}
		
		public Output<JsonObject> run(String function, String argument) throws Exception {
			return run(function, read(argument), new HashMap<String, String>());
		}

		private JsonObject read(String argument) {
			return Json.createReader(new StringReader(argument)).readObject();
		}
		
		public Output<JsonObject> run(String function) throws Exception {
			return run(function, Json.createObjectBuilder().build(), new HashMap<String, String>());
		}
		
		public Output<JsonObject> run(String function, JsonObject argument) throws Exception {
			return run(function, argument, new HashMap<String, String>());
		}
		
		public Output<JsonObject> run(String function, JsonObject argument, Map<String, String> properties) throws Exception {
			return execute(function, argument, properties);
		}

		private Output<JsonObject> execute(String function, JsonObject argument, Map<String, String> properties) throws Exception {
			Input<JsonObject> input = new Input<>();
			input.setFunction(function);
			input.setPayload(argument);			
			StringBuilder classes = new StringBuilder();
			functionClasses.forEach(cl->{classes.append(cl.getName()+KeywordExecutor.KEYWORD_CLASSES_DELIMITER);});
			properties.put(KeywordExecutor.KEYWORD_CLASSES, classes.toString());
			input.setProperties(properties);
			
			Map<String, String> allProperties = new HashMap<>();
			allProperties.putAll(contextProperties);
			allProperties.putAll(properties);
			Output<JsonObject> output;
			try {
				output = keywordExecutor.handle(input, tokenSession, tokenReservationSession, allProperties, null);
				if(output.getError()!=null) {
					logger.error("Keyword error occurred:"+output.getError());
				}
				return output;
			} catch (Exception e) {
				throw e;
			}
		}
		
		public void close() {
			tokenSession.close();
			tokenReservationSession.close();
		}
	}
	
	public static ExecutionContext getExecutionContext(Class<?>... functionClass) {
		return getExecutionContext(new HashMap<>(), functionClass);
	}
	
	public static ExecutionContext getExecutionContext(Map<String, String> properties, Class<?>... keywordClass) {
		if(keywordClass.length==0) {
			throw new RuntimeException("Please specify at least one class containing the keyword definitions");
		}
		return new ExecutionContext(Arrays.asList(keywordClass), properties, true);
	}
	
}
