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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.functions.io.AbstractSession;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;
import step.reporting.LiveReporting;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeywordExecutor {
	
	public static final String VALIDATE_PROPERTIES = "$validateProperties";
	public static final String KEYWORD_CLASSES = "$keywordClasses";
	public static final String KEYWORD_CLASSES_DELIMITER = ";";
	
	private static final Logger logger = LoggerFactory.getLogger(KeywordExecutor.class);
	
	private boolean throwExceptionOnError = false;
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(.+?)\\}");

	private final LiveReporting liveReporting;

	public KeywordExecutor(boolean throwExceptionOnError) {
		this(throwExceptionOnError, new LiveReporting(null, null));
	}

	public KeywordExecutor(boolean throwExceptionOnError, LiveReporting liveReporting) {
		super();
		this.throwExceptionOnError = throwExceptionOnError;
		this.liveReporting = liveReporting;
	}

	public boolean isThrowExceptionOnError() {
		return throwExceptionOnError;
	}

	public void setThrowExceptionOnError(boolean throwExceptionOnError) {
		this.throwExceptionOnError = throwExceptionOnError;
	}

	public Output<JsonObject> handle(Input<JsonObject> input, AbstractSession tokenSession, AbstractSession tokenReservationSession, Map<String, String> properties, AutomationPackageFileSupplier automationPackageFileSupplier) throws Exception {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		
		String kwClassnames = input.getProperties().get(KEYWORD_CLASSES);
		if(kwClassnames != null && kwClassnames.trim().length()>0) {
			for(String kwClassname:kwClassnames.split(KEYWORD_CLASSES_DELIMITER)) {
				Class<?> kwClass = cl.loadClass(kwClassname);
				
				for (Method m : kwClass.getDeclaredMethods()) {
					if(m.isAnnotationPresent(Keyword.class)) {
						Keyword annotation = m.getAnnotation(Keyword.class);
						String keywordName = getKeywordName(m, annotation);
						if (keywordName.equals(input.getFunction())) {
							return executeKeyword(keywordName, input.getPayload(), tokenSession, tokenReservationSession, properties, m, annotation, null, automationPackageFileSupplier);
						}
					}
				}
			}
		}

		throw new Exception("Unable to find method annotated by '" + Keyword.class.getName() + "' with name=='"+ input.getFunction() + "'");
	}

	protected Output<JsonObject> executeKeyword(AbstractSession tokenSession, AbstractSession tokenReservationSession, Map<String, String> properties, Method method, Object[] args, Keyword annotation, Consumer<Object> returnKeywordCallback, AutomationPackageFileSupplier automationPackageFileSupplier) throws Exception {
		Parameter[] parameters = method.getParameters();
		JsonObject inputPayload = getJsonInputFromMethodParameters(args, parameters);
		String keywordName = getKeywordName(method, annotation);
		return executeKeyword(keywordName, inputPayload, tokenSession, tokenReservationSession, properties, method, annotation, returnKeywordCallback, automationPackageFileSupplier);
	}

	protected Output<JsonObject> executeKeyword(String keywordName, JsonObject inputPayload, AbstractSession tokenSession, AbstractSession tokenReservationSession, Map<String, String> properties, Method method, Keyword annotation, Consumer<Object> returnKeywordCallback, AutomationPackageFileSupplier automationPackageFileSupplier) throws Exception {

		Map<String, String> keywordProperties;
		if(properties.containsKey(VALIDATE_PROPERTIES)) {
			String[] requiredPropertyKeys = annotation.properties();
			String[] optionalPropertyKeys = annotation.optionalProperties();
			List<String> missingProperties = new ArrayList<>();
			Map<String, String> reducedProperties = new HashMap<>();
			try {
				processPropertyKeys(properties, inputPayload, requiredPropertyKeys, missingProperties, reducedProperties, true);
				processPropertyKeys(properties, inputPayload, optionalPropertyKeys, missingProperties, reducedProperties, false);

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

		return invokeMethod(keywordName, method, inputPayload, tokenSession, tokenReservationSession, keywordProperties, returnKeywordCallback, automationPackageFileSupplier);
	}

	public static String getKeywordName(Method m, Keyword annotation) {
		String annotatedFunctionName = annotation.name();
		String keywordName;
		if ((annotatedFunctionName == null || annotatedFunctionName.length() == 0)) {
			keywordName = m.getName();
		} else {
			keywordName = annotatedFunctionName;
		}
		return keywordName;
	}

	private void processPropertyKeys(Map<String, String> properties, JsonObject inputPayload, String[] requiredPropertyKeys, List<String> missingProperties,
			Map<String, String> reducedProperties, boolean required) throws MissingPlaceholderException {
		// First try to resolve the placeholders
		List<String> resolvedPropertyKeys = new ArrayList<>();
		for (String key : requiredPropertyKeys) {
			resolvedPropertyKeys.add(replacePlaceholders(key, properties, inputPayload));
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
	
	private String replacePlaceholders(String string, Map<String, String> properties, JsonObject inputPayload) throws MissingPlaceholderException {
		StringBuffer sb = new StringBuffer();
		Matcher m = PLACEHOLDER_PATTERN.matcher(string);
		while (m.find()) {
            String key = m.group(1);
            String replacement;
            if(inputPayload.containsKey(key)) {
        		replacement = inputPayload.getString(key);
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

	private Output<JsonObject> invokeMethod(String keywordName, Method m, JsonObject inputPayload, AbstractSession tokenSession, AbstractSession tokenReservationSession, Map<String, String> properties, Consumer<Object> returnKeywordCallback, AutomationPackageFileSupplier automationPackageFileSupplier)
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
			script.setInput(inputPayload);
			script.setProperties(properties);
			script.setOutputBuilder(outputBuilder);
            script.setAutomationPackageFileSupplier(automationPackageFileSupplier);
			script.liveReporting = liveReporting;

			Keyword annotation = m.getAnnotation(Keyword.class);
			try {
				script.beforeKeyword(keywordName, annotation);
				Object outputPojo = m.invoke(instance, resolveMethodArguments(script.getInput(), m));
				if(outputPojo != null) {
					if (outputBuilder.getPayload() != null || outputBuilder.getPayloadJson() != null ||
							(outputBuilder.getPayloadBuilder() != null && !outputBuilder.getPayloadBuilder().build().isEmpty())) {
						throw new RuntimeException("This keyword function returns a value but also uses 'output' to define the output payload. This is not allowed because returned values override the output payload.");
					}
					JsonObject outputPayload = JsonObjectMapper.javaObjectToJsonObject(outputPojo);
					outputBuilder.setPayload(outputPayload);
				}
				if (returnKeywordCallback != null) {
					returnKeywordCallback.accept(outputPojo);
				}
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

	private Object[] resolveMethodArguments(JsonObject input, Method m) {
		List<Object> res = new ArrayList<>();
		Parameter[] parameters = m.getParameters();
		if(parameters.length == 1 && parameters[0] != null && !parameters[0].isAnnotationPresent(step.handlers.javahandler.Input.class)) {
			res.add(JsonObjectMapper.jsonValueToJavaObject(input, parameters[0].getType()));
		} else {
			for (Parameter p : parameters) {
				if (p.isAnnotationPresent(step.handlers.javahandler.Input.class)) {
					step.handlers.javahandler.Input annotation = p.getAnnotation(step.handlers.javahandler.Input.class);
					String name = annotation.name() == null || annotation.name().isEmpty() ? p.getName() : annotation.name();
					if(input.containsKey(name)) {
						res.add(JsonObjectMapper.jsonValueToJavaObject(input.getOrDefault(name, null), p.getParameterizedType()));
					} else {
						if(annotation.required()) {
							throw new RuntimeException("Missing required input '" + name + "'");
						} else {
							String defaultValue = annotation.defaultValue();
							if(defaultValue != null && !defaultValue.isEmpty()) {
								res.add(SimplifiedObjectDeserializer.parse(defaultValue, p.getParameterizedType()));
							} else {
								res.add(null);
							}
						}
					}
				} else {
					res.add(null);
				}
			}
		}
		return res.toArray();
	}

	/**
	 * Builds the Keyword input object as Json based on the method parameters of the Keyword call
	 * @param args
	 * @param parameters
	 * @return
	 */
	public static JsonObject getJsonInputFromMethodParameters(Object[] args, Parameter[] parameters) {
		JsonObjectBuilder inputBuilder = Json.createObjectBuilder();
		if (parameters.length != args.length) {
			throw new IllegalStateException(String.format("The number of args %d differs from the number of parameters %d", args.length, parameters.length));
		}
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			step.handlers.javahandler.Input input = parameter.getAnnotation(step.handlers.javahandler.Input.class);
			JsonObjectMapper.addValueToJsonObject(inputBuilder, input.name(), args[i]);
		}
		return inputBuilder.build();
	}

}
