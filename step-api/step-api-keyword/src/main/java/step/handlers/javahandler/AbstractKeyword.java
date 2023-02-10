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

import java.util.Map;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.functions.io.AbstractSession;
import step.functions.io.OutputBuilder;

public class AbstractKeyword {
	
	protected Logger logger = LoggerFactory.getLogger(AbstractKeyword.class);

	protected OutputBuilder output;
	
	protected JsonObject input;
	
	protected Map<String, String> properties;
	
	protected AbstractSession session;
	
	protected AbstractSession tokenSession;
	
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
}
