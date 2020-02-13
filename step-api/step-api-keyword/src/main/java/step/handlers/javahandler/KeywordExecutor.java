/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.handlers.javahandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.functions.io.AbstractSession;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;

public class KeywordExecutor {
	
	public static final String VALIDATE_PROPERTIES = "$validateProperties";
	public static final String KEYWORD_CLASSES = "$keywordClasses";
	public static final String KEYWORD_CLASSES_DELIMITER = ";";
	
	private static final Logger logger = LoggerFactory.getLogger(KeywordExecutor.class);
	
	private boolean throwExceptionOnError = false;
	private static final Pattern p = Pattern.compile("(.*)\\{(.*)\\}(.*)");

	public KeywordExecutor(boolean throwExceptionOnError) {
		super();
		this.throwExceptionOnError = throwExceptionOnError;
	}

	public boolean isThrowExceptionOnError() {
		return throwExceptionOnError;
	}

	public void setThrowExceptionOnError(boolean throwExceptionOnError) {
		this.throwExceptionOnError = throwExceptionOnError;
	}

	public Output<JsonObject> handle(Input<JsonObject> input, AbstractSession tokenSession, AbstractSession tokenReservationSession, Map<String, String> properties) throws Exception {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		
		String kwClassnames = input.getProperties().get(KEYWORD_CLASSES);
		if(kwClassnames != null && kwClassnames.trim().length()>0) {
			for(String kwClassname:kwClassnames.split(KEYWORD_CLASSES_DELIMITER)) {
				Class<?> kwClass = cl.loadClass(kwClassname);
				
				for (Method m : kwClass.getDeclaredMethods()) {
					if(m.isAnnotationPresent(Keyword.class)) {
						Keyword annotation = m.getAnnotation(Keyword.class);
						String annotatedFunctionName = annotation.name();
						if (((annotatedFunctionName == null || annotatedFunctionName.length() == 0)
								&& m.getName().equals(input.getFunction()))
								|| annotatedFunctionName.equals(input.getFunction())) {
							
							Map<String, String> keywordProperties;
							if(properties.containsKey(VALIDATE_PROPERTIES)) {
								String[] requiredPropertyKeys = annotation.properties();
								String[] optionalPropertyKeys = annotation.optionalProperties();
								List<String> missingProperties = new ArrayList<>();
								Map<String, String> reducedProperties = new HashMap<>();
								processPropertyKeys(properties, input, requiredPropertyKeys, missingProperties, reducedProperties, true);
								processPropertyKeys(properties, input, optionalPropertyKeys, missingProperties, reducedProperties, false);
								
								if(missingProperties.size()>0) {
									OutputBuilder outputBuilder = new OutputBuilder();
									outputBuilder.setBusinessError("The Keyword is missing the following properties "+missingProperties.toString());
									return outputBuilder.build();
								} else {
									keywordProperties = reducedProperties;
								}
							} else {
								keywordProperties = properties;
							}
							
							return invokeMethod(m, input, tokenSession, tokenReservationSession, keywordProperties);
						}
					}
				}
			}
		}

		throw new Exception("Unable to find method annoted by '" + Keyword.class.getName() + "' with name=='"+ input.getFunction() + "'");
	}

	private void processPropertyKeys(Map<String, String> properties, Input<JsonObject> input, String[] reducedPropertyKeys, List<String> missingProperties,
			Map<String, String> reducedProperties, boolean required) {
		for (String string : reducedPropertyKeys) {
			if(!properties.containsKey(string)) {
				//if the property uses place holder and place holder is a property 
				Matcher ma = p.matcher(string);
				if (ma.matches() && (properties.containsKey(ma.group(2)) || input.getPayload().containsKey(ma.group(2)))) {
					String value = (properties.containsKey(ma.group(2))) ? properties.get(ma.group(2)) : input.getPayload().getString(ma.group(2));  
					String resolvedName = ma.group(1) + value + ma.group(3);
					if (properties.containsKey(resolvedName)) {
						reducedProperties.put(resolvedName, properties.get(resolvedName));
					} else if (required) {
						missingProperties.add(string + " or " + resolvedName);
					}
						
				} else if (required) {
					missingProperties.add(string);
				}
					
			} else {
				reducedProperties.put(string, properties.get(string));
			}
		}
		
	}

	private Output<JsonObject> invokeMethod(Method m, Input<JsonObject> input, AbstractSession tokenSession, AbstractSession tokenReservationSession, Map<String, String> properties)
			throws Exception {
		Class<?> clazz = m.getDeclaringClass();
		Object instance = clazz.newInstance();

		if (logger.isDebugEnabled()) {
			logger.debug("Invoking method " + m.getName() + " from class " + clazz.getName() + " loaded by "
					+ clazz.getClassLoader().toString());
		}

		OutputBuilder outputBuilder = new OutputBuilder();

		if (instance instanceof AbstractKeyword) {
			AbstractKeyword script = (AbstractKeyword) instance;
			script.setTokenSession(tokenSession);
			script.setSession(tokenReservationSession);
			script.setInput(input.getPayload());
			script.setProperties(properties);
			script.setOutputBuilder(outputBuilder);

			try {
				m.invoke(instance);
			} catch (Exception e) {
				boolean throwException = script.onError(e);
				if (throwException) {
					Throwable cause = e.getCause();
					Throwable reportedEx;
					if(e instanceof InvocationTargetException && cause!=null && cause instanceof Throwable) {
						reportedEx = cause;
					} else {
						reportedEx = e;
					}
					outputBuilder.setError(reportedEx.getMessage()!=null?reportedEx.getMessage():"Empty error message", reportedEx);
					if(throwExceptionOnError) {
						Output<?> output = outputBuilder.build();
						throw new KeywordException(output, reportedEx);
					}
				}
			} finally {
				// TODO error handling
			}
		} else {
			outputBuilder.add("Info", "The class '" + clazz.getName() + "' doesn't extend '" + AbstractKeyword.class.getName()
					+ "'. Extend this class to get input parameters from STEP and return output.");
		}
		
		Output<JsonObject> output = outputBuilder.build();
		if(throwExceptionOnError && output.getError() != null) {
			throw new KeywordException(output);
		} else {
			return output;
		}
	}
}
