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
import step.functions.io.OutputBuilder;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.File;
import java.io.StringReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AbstractKeyword {
	
	protected Logger logger = LoggerFactory.getLogger(AbstractKeyword.class);

	protected OutputBuilder output;

	/**
	 * This field is set with the Keyword input as JSON when a Keyword is called.
	 */
	protected JsonObject input;

	/**
	 * This field is set with a merge of all properties (Variables defined in the Plan calling the Keyword,
	 * Parameters, and Agent properties) when a Keyword is called.
	 */
	protected Map<String, String> properties;

	/**
	 * This field is set with the session object when a Keyword is called. The lifecycle of this session object matches
	 * the lifecycle of the corresponding Session defined in the Plan.
	 */
	protected AbstractSession session;

	/**
	 * This field is set with the token session object when a Keyword is called. The lifecycle of the token session
	 * object matches the lifecycle of the Agent process
	 */
	protected AbstractSession tokenSession;

	private AutomationPackageFileSupplier automationPackageFileSupplier;
	
	public AbstractSession getSession() {
		return session;
	}

	public void setSession(AbstractSession session) {
		this.session = session;
	}

	public AbstractSession getTokenSession() {
		return tokenSession;
	}

	public void setTokenSession(AbstractSession tokenSession) {
		this.tokenSession = tokenSession;
	}

	/**
	 * @return the Keyword input object as JSON
	 */
	public JsonObject getInput() {
		return input;
	}

	public void setInput(JsonObject input) {
		this.input = input;
	}

	public OutputBuilder getOutputBuilder() {
		return output;
	}

	public void setOutputBuilder(OutputBuilder outputBuilder) {
		this.output = outputBuilder;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public void setAutomationPackageFileSupplier(AutomationPackageFileSupplier automationPackageFileSupplier) {
		this.automationPackageFileSupplier = automationPackageFileSupplier;
	}

	/**
	 * Hook called when an exception is thrown by a keyword or by the beforeKeyword hook
	 *
	 * @param e the exception thrown
	 * @return true if the exception passed as argument has to be rethrown.
	 * Set to false if the error has already been handled by this hook and shouldn't be handled.
	 */
	public boolean onError(Exception e) {
		return true;
	}

	/**
	 * Hook called before each keyword call.
	 * If an exception is thrown by this method, the keyword won't be executed (but afterKeyword and onError will)
	 *
	 * @param keywordName the name of the keyword. Will be the function name if annotation.name() is empty
	 * @param annotation the annotation of the called keyword
	 */
	public void beforeKeyword(String keywordName, Keyword annotation) {}

	/**
	 * Hook called after each keyword call.
	 * This method is always called. If an exception is thrown by the keyword or the beforeKeyword hook,
	 * this method is called after the onError hook.
	 *
	 * @param keywordName the name of the keyword. Will be the method name if annotation.name() is empty
	 * @param annotation the annotation of the called keyword
	 */
	public void afterKeyword(String keywordName, Keyword annotation) {}


	/**
	 * Retrieves the String value of a specified keyword input.
	 * If the input is not found, the method attempts to retrieve the value
	 * from the property map. If neither source contains the key, it returns null.
	 * @param key the key of the input or property
	 * @return
	 */
	protected String getInputOrProperty(String key) {
		return getInputOrProperty_(key, input::getString, properties::get);
	}

	/**
	 * Retrieves the Integer value of a specified keyword input.
	 * If the input is not found, the method attempts to retrieve the value
	 * from the property map. If neither source contains the key, it returns null.
	 * @param key the key of the input or property
	 * @return
	 */
	protected Integer getInputOrPropertyAsInteger(String key) {
		return getInputOrProperty_(key, input::getInt, k -> Integer.parseInt(properties.get(key)));
	}

	/**
	 * Retrieves the Long value of a specified keyword input.
	 * If the input is not found, the method attempts to retrieve the value
	 * from the property map. If neither source contains the key, it returns null.
	 * @param key the key of the input or property
	 * @return
	 */
	protected Long getInputOrPropertyAsLong(String key) {
		return getInputOrProperty_(key, k -> input.getJsonNumber(k).longValue(), k -> Long.parseLong(properties.get(key)));
	}

	/**
	 * Retrieves the Boolean value of a specified keyword input.
	 * If the input is not found, the method attempts to retrieve the value
	 * from the property map. If neither source contains the key, it returns null.
	 * @param key the key of the input or property
	 * @return
	 */
	protected Boolean getInputOrPropertyAsBoolean(String key) {
		return getInputOrProperty_(key, input::getBoolean, k -> Boolean.parseBoolean(properties.get(key)));
	}

	/**
	 * Retrieves the value of a specified keyword input as object.
	 * If the input is not found, the method attempts to retrieve the value
	 * from the property map. If neither source contains the key, it returns null.
	 * @param key the key of the input or property
	 * @param valueType the {@link java.lang.reflect.Type} representing the class of the result object to which
	 *                     the JSON will be mapped
	 * @return the mapped object of the specified type, populated with data from the JSON
	 */
	protected <T> T getInputOrPropertyAsObject(String key, Class<T> valueType) {
		return (T) JsonObjectMapper.jsonValueToJavaObject(getInputOrProperty_(key, input::getJsonObject, k ->
				Json.createReader(new StringReader(properties.get(key))).readObject()), valueType);
	}

	/**
	 * Retrieves the value of a specified keyword input as list.
	 * If the input is not found, the method attempts to retrieve the value
	 * from the property map. If neither source contains the key, it returns null.
	 * @param key the key of the input or property
	 * @param valueType The {@link java.lang.reflect.Type} representing the class of the elements in the list to which
	 *                    each item in the JSON array will be mapped.
	 * @return a {@link List} containing the mapped objects of the specified type,
	 * where each item in the JSON array is converted into an element in the list.
	 */
	protected <T> List<T> getInputOrPropertyAsList(String key, Class<T> valueType) {
		Type listType = new ParameterizedType() {
			@Override
			public Type[] getActualTypeArguments() {
				return new Type[]{valueType};
			}

			@Override
			public Type getRawType() {
				return ArrayList.class;
			}

			@Override
			public Type getOwnerType() {
				return null;
			}
		};

		return (List<T>) JsonObjectMapper.jsonValueToJavaObject(getInputOrProperty_(key, input::getJsonArray, k ->
				Json.createReader(new StringReader(properties.get(key))).readArray()), listType);
	}

	private <T extends Object> T getInputOrProperty_(String key, Function<String, T> inputProvider, Function<String, T> propertyProvider) {
		if(input.containsKey(key)) {
			return inputProvider.apply(key);
		} else {
			if(properties.containsKey(key)) {
				return propertyProvider.apply(key);
			} else {
				return null;
			}
		}
	}

	/**
	 * @return the extracted automation package content if this Keyword instance is part of an automation package.
	 * If this Keyword instance is not part of an automation package, this method returns null.
	 */
	protected File retrieveAndExtractAutomationPackage() {
		return automationPackageFileSupplier != null ? automationPackageFileSupplier.retrieveAndExtractAutomationPackage() : null;
	}

	/**
	 * @return true if this Keyword instance is part of an automation package
	 */
	protected boolean isInAutomationPackage() {
		return automationPackageFileSupplier != null && automationPackageFileSupplier.hasAutomationPackageFile();
	}
}
