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
package step.functions.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import step.grid.io.Attachment;

public class Input<IN> {

	protected String function;
	
	protected long functionCallTimeout;
	
	protected IN payload;
	
	protected Map<String, String> properties;
	
	private List<Attachment> attachments = new ArrayList<Attachment>();

	/**
	 * @return the name of the function (keyword) to be executed
	 */
	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	/**
	 * @return the call timeout of the function in ms
	 */
	public long getFunctionCallTimeout() {
		return functionCallTimeout;
	}

	/**
	 * @param functionCallTimeout the call timeout of the function in ms
	 */
	public void setFunctionCallTimeout(long functionCallTimeout) {
		this.functionCallTimeout = functionCallTimeout;
	}

	/**
	 * @return the function payload
	 */
	public IN getPayload() {
		return payload;
	}

	public void setPayload(IN payload) {
		this.payload = payload;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	
	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}
}
