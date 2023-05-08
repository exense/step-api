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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
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
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(.+?)\\}");

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
								try {
									processPropertyKeys(properties, input, requiredPropertyKeys, missingProperties, reducedProperties, true);
									processPropertyKeys(properties, input, optionalPropertyKeys, missingProperties, reducedProperties, false);
									
									if(missingProperties.size()>0) {
										OutputBuilder outputBuilder = new OutputBuilder();
										outputBuilder.setBusinessError("The Keyword is missing the following properties "+missingProperties.toString());
										return outputBuilder.build();
									} else {
										keywordProperties = reducedProperties;
									}
								} catch (MissingPlaceholderException e) {
									OutputBuilder outputBuilder = new OutputBuilder();
									outputBuilder.setBusinessError("The Keyword is missing the following property or input '"+e.placeholder+"'");
									return outputBuilder.build();
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

		throw new Exception("Unable to find method annotated by '" + Keyword.class.getName() + "' with name=='"+ input.getFunction() + "'");
	}

	private void processPropertyKeys(Map<String, String> properties, Input<JsonObject> input, String[] requiredPropertyKeys, List<String> missingProperties,
			Map<String, String> reducedProperties, boolean required) throws MissingPlaceholderException {
		// First try to resolve the placeholders
		List<String> resolvedPropertyKeys = new ArrayList<>();
		for (String key : requiredPropertyKeys) {
			resolvedPropertyKeys.add(replacePlaceholders(key, properties, input));
		}
		
		// Then check if all required properties exist
		for (String string : resolvedPropertyKeys) {
			if(properties.containsKey(string)) { 
				reducedProperties.put(string, properties.get(string));
			} else {
				if(required) {
					missingProperties.add(string);
				}
			}
		}
	}
	
	private String replacePlaceholders(String string, Map<String, String> properties, Input<JsonObject> input) throws MissingPlaceholderException {
		StringBuffer sb = new StringBuffer();
		Matcher m = PLACEHOLDER_PATTERN.matcher(string);
		while (m.find()) {
            String key = m.group(1);
            String replacement;
            if(input.getPayload().containsKey(key)) {
        		replacement = input.getPayload().getString(key);
            } else {
            	if(properties.containsKey(key)) {
                	replacement = properties.get(key);
            	} else {
            		throw new MissingPlaceholderException(key);
            	}
            }
        	m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
		m.appendTail(sb);
		return sb.toString();
	}
	
	@SuppressWarnings({"serial" })
	private static class MissingPlaceholderException extends Exception {
		
		String placeholder;

		public MissingPlaceholderException(String placeholder) {
			super();
			this.placeholder = placeholder;
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

			Keyword annotation = m.getAnnotation(Keyword.class);
			String keywordName = input.getFunction();
			try {
				script.beforeKeyword(keywordName,annotation);
				m.invoke(instance, resolveMethodArguments(script.getInput(), m));
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
				script.afterKeyword(keywordName, annotation);
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

	private Object[] resolveMethodArguments(JsonObject input, Method m) throws Exception {
		List<Object> res = new ArrayList<>();
		for (Parameter p : m.getParameters()) {
			if (p.isAnnotationPresent(step.handlers.javahandler.Input.class)) {
				step.handlers.javahandler.Input annotation = p.getAnnotation(step.handlers.javahandler.Input.class);
				String name = annotation.name() == null || annotation.name().isEmpty() ? p.getName() : annotation.name();
				res.add(JsonInputConverter.getValueFromJsonInput(input, name, p.getParameterizedType()));
			} else {
				res.add(null);
			}
		}
		return res.toArray();
	}

}
