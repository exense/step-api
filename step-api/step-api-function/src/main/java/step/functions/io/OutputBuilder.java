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

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.json.*;
import javax.json.spi.JsonProvider;

import step.core.reports.Error;
import step.core.reports.ErrorType;
import step.core.reports.Measure;
import step.core.reports.MeasurementsBuilder;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;

/**
 * A builder for Output instances.
 *
 */
public class OutputBuilder {
	
	private JsonObjectBuilder payloadBuilder;
	
	private String payloadJson;
	private JsonObject payload;

	private MeasurementsBuilder measureHelper;
	
	private Error error;
	
	private List<Attachment> attachments;
	
	private static JsonProvider jprov = JsonProvider.provider();
	
	private Measure lastMeasureHandle = null;

	public OutputBuilder() {
		super();
		
		payloadBuilder = jprov.createObjectBuilder();

		measureHelper = new MeasurementsBuilder();
	}
	
	public JsonObjectBuilder getPayloadBuilder() {
		return payloadBuilder;
	}

	public void setPayloadBuilder(JsonObjectBuilder payloadBuilder) {
		this.payloadBuilder = payloadBuilder;
	}

	/**
	 * Adds an output attribute  
	 * If the object contains a mapping for the specified name, this method replaces the old value with the specified value.
	 * 
	 * @param name the name of the output attribute
	 * @param value the value of the output attribute
	 * @return this instance
	 */
	public OutputBuilder add(String name, boolean value) {
		payloadBuilder.add(name, value);
		return this;
	}

	/**
	 * Adds an output attribute  
	 * If the object contains a mapping for the specified name, this method replaces the old value with the specified value.
	 * 
	 * @param name the name of the output attribute
	 * @param value the value of the output attribute
	 * @return this instance
	 */
	public OutputBuilder add(String name, double value) {
		payloadBuilder.add(name, value);
		return this;
	}

	/**
	 * Adds an output attribute  
	 * If the object contains a mapping for the specified name, this method replaces the old value with the specified value.
	 * 
	 * @param name the name of the output attribute
	 * @param value the value of the output attribute
	 * @return this instance
	 */
	public OutputBuilder add(String name, int value) {
		payloadBuilder.add(name, value);
		return this;
	}

	/**
	 * Adds an output attribute  
	 * If the object contains a mapping for the specified name, this method replaces the old value with the specified value.
	 * 
	 * @param name the name of the output attribute
	 * @param value the value of the output attribute
	 * @return this instance
	 */
	public OutputBuilder add(String name, long value) {
		payloadBuilder.add(name, value);
		return this;
	}
	
	/**
	 * Adds an output attribute  
	 * If the object contains a mapping for the specified name, this method replaces the old value with the specified value.
	 * 
	 * @param name the name of the output attribute
	 * @param value the value of the output attribute
	 * @return this instance
	 */
	public OutputBuilder add(String name, String value) {
		payloadBuilder.add(name, value);
		return this;
	}

	private OutputBuilder add(String name, JsonValue jsonValue) {
		payloadBuilder.add(name, jsonValue);
		return this;
	}

	/**
	 * Reports a technical error. This will be reported as ERROR in STEP
	 * 
	 * @param technicalError the error message of the technical error
	 * @return this instance
	 */
	public OutputBuilder setError(String technicalError) {
		error = new Error(ErrorType.TECHNICAL, "keyword", technicalError, 0, true);
		return this;
	}
	
	/**
	 * Appends a technical error message.
	 * Calling this method for the first time will have the same effect as calling setError
	 * 
	 * @param technicalError the error message of the technical error
	 * @return this instance
	 */
	public OutputBuilder appendError(String technicalError) {
		if(error!=null) {
			error.setMsg(error.getMsg()+technicalError);			
		} else {
			setError(technicalError);
		}
		return this;
	}
	
	/**
	 * Reports a technical error and appends the exception causing this error
	 * as attachment
	 * 
	 * @param errorMessage the error message of the technical error
	 * @param e the exception that caused the technical error
	 * @return this instance
	 */
	public OutputBuilder setError(String errorMessage, Throwable e) {
		setError(errorMessage);
		addAttachment(generateAttachmentForException(e));
		return this;
	}
	
	/**
	 * Reports a business error. This will be reported as FAILED in STEP
	 * 
	 * @param businessError the error message of the business error
	 * @return this instance
	 */
	public OutputBuilder setBusinessError(String businessError) {
		error = new Error(ErrorType.BUSINESS, "keyword", businessError, 0, true);
		return this;
	}
	
	public OutputBuilder setError(Error error) {
		this.error = error;
		return this;
	}
	
	/**
	 * @return the payload of this output. This has no eff
	 * 
	 */
	public String getPayloadJson() {
		return payloadJson;
	}

	/**
	 * 
	 * @param payloadJson the payload of this output.
	 */
	public void setPayloadJson(String payloadJson) {
		this.payloadJson = payloadJson;
	}

	public JsonObject getPayload() {
		return payload;
	}

	public void setPayload(JsonObject payload) {
		this.payload = payload;
	}

	/**
	 * Adds attachments to the output
	 * 
	 * @param attachments the list of attachments to be added to the output
	 */
	public void addAttachments(List<Attachment> attachments) {
		createAttachmentListIfNeeded();
		attachments.addAll(attachments);
	}

	/**
	 * Adds an attachment to the output
	 * 
	 * @param attachment the attachment to be added to the output
	 */
	public void addAttachment(Attachment attachment) {
		createAttachmentListIfNeeded();
		attachments.add(attachment);
	}

	private void createAttachmentListIfNeeded() {
		if(attachments==null) {
			attachments = new ArrayList<>();
		}
	}
	
	/**
	 * Starts a performance measurement. The current time will be used as starttime
	 * 
	 * @param id a unique identifier of the measurement
	 */
	public void startMeasure(String id) {
		measureHelper.startMeasure(id);
	}

	/**
	 * Starts a performance measurement
	 * 
	 * @param id a unique identifier of the measurement
	 * @param begin the start time of the measurement
	 */
	public void startMeasure(String id, long begin) {
		measureHelper.startMeasure(id, begin);
	}

	/**
	 * Adds a performance measurement
	 * 
	 * @param measureName a unique identifier of the measurement
	 * @param durationMillis the duration of the measurement in ms
	 */
	public void addMeasure(String measureName, long durationMillis) {
		measureHelper.addMeasure(measureName, durationMillis);
	}

	/**
	 * Adds a performance measurement
	 *
	 * @param measureName a unique identifier of the measurement
	 * @param durationMillis the duration of the measurement in ms
	 * @param begin the start timestamp of the measurement in ms
	 */
	public void addMeasure(String measureName, long durationMillis, long begin) {
		measureHelper.addMeasure(measureName, durationMillis, begin);
	}

	/**
	 * Adds a performance measurement with custom data
	 * 
	 * @param measureName a unique identifier of the measurement
	 * @param aDurationMillis the duration of the measurement in ms
	 * @param data the custom data of the measurement
	 */
	public void addMeasure(String measureName, long aDurationMillis, Map<String, Object> data) {
		measureHelper.addMeasure(measureName, aDurationMillis, data);
	}

	/**
	 * Adds a performance measurement
	 *
	 * @param measure the performance measurement to be added
	 */
	public void addMeasure(Measure measure) {
		measureHelper.addMeasure(measure);
	}

    /**
     * Adds a performance measurement with custom data
     *
     * @param measureName a unique identifier of the measurement
     * @param aDurationMillis the duration of the measurement in ms
     * @param begin the start timestamp of the measurement in ms
     * @param data the custom data of the measurement
     */
    public void addMeasure(String measureName, long aDurationMillis, long begin, Map<String, Object> data) {
        measureHelper.addMeasure(measureName, aDurationMillis, begin, data);
    }

	/**
	 * Stops the current performance measurement and adds it to the output
	 */
	public void stopMeasure() {
		measureHelper.stopMeasure();
	}

	/**
	 * Stops the current performance measurement and adds it to the output. 
	 * 
	 * @param data custom data to be added to the measurement
	 */
	public void stopMeasure(Map<String, Object> data) {
		measureHelper.stopMeasure(data);
	}
	
	public void stopMeasureForAdditionalData() {
		this.lastMeasureHandle = measureHelper.stopMeasure();
	}
	
	public void setLastMeasureAdditionalData(Map<String, Object> data) {
		this.lastMeasureHandle.setData(data);
		this.lastMeasureHandle = null;
	}

	/**
	 * Builds the output instance
	 * 
	 * @return the output message
	 */
	public Output<JsonObject> build() {
		Output<JsonObject> message = new Output<>();
		JsonObject payload;
		if (this.payload != null) {
			payload = this.payload;
		} else {
			if (payloadJson == null) {
				payload = payloadBuilder.build();
			} else {
				JsonReader reader = Json.createReader(new StringReader(payloadJson));
				try {
					payload = reader.readObject();
				} finally {
					reader.close();
				}
			}
		}
		message.setPayload(payload);
		message.setMeasures(measureHelper.getMeasures());
		message.setAttachments(attachments);
		message.setError(error);
		return message;
	}

	private Attachment generateAttachmentForException(Throwable e) {
		Attachment attachment = new Attachment();	
		attachment.setName("exception.log");
		StringWriter w = new StringWriter();
		e.printStackTrace(new PrintWriter(w));
		attachment.setHexContent(AttachmentHelper.getHex(w.toString().getBytes()));
		return attachment;
	}

	public void mergeOutput(Output<JsonObject> output) {
		output.getPayload().forEach(this::add);
		output.getMeasures().forEach(this::addMeasure);
		output.getAttachments().forEach(this::addAttachment);
		if(output.getError() != null) {
			this.appendError(output.getError().getMsg());
		}
	}
}
