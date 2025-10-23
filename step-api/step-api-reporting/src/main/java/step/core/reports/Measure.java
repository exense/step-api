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
package step.core.reports;

import java.util.Map;


/**
 * Represents a measurement of performance or execution metrics for a given operation.
 * <p>
 * A {@code Measure} instance captures the name of the measured operation(s), start time,
 * total duration, optional additional contextual data, and a status describing the result
 * (e.g. passed, failed, or technical error).
 * </p>
 *
 * <b>Implementation / behavior note</b>
 * <p>
 * Measures are reported either 1) at the end of a keyword call (via a {@link MeasurementsBuilder} object),
 * or 2) live, while a keyword call is performed, via a {@link  step.reporting.LiveMeasures} object.
 * <p>
 * In the former case (1), measures that don't have an explicit status set will be assigned the same
 * status as the (finished) keyword they belong to.
 * <p>
 * In the latter case (2), measures <b>must</b> carry their own status, because the keyword execution
 * has not finished yet (and therefore has no final status) at the time when the measure is processed.
 *
 * @see step.reporting.LiveMeasures
 * @see MeasurementsBuilder
 *
 */
public class Measure {

	/**
	 * Possible outcomes for a measure or operation.
	 */
	public enum Status {
		/**
		 * The operation completed successfully.
		 */
		PASSED,

		/**
		 * The operation completed but did not meet expected conditions.
		 */
		FAILED,

		/**
		 * The operation failed due to a technical or unexpected error.
		 */
		TECHNICAL_ERROR,
	}

	/**
	 * The name or identifier of the measure or operation.
	 */
	private String name;

	/**
	 * The duration of the measure in milliseconds.
	 */
	private long duration;

	/**
	 * The timestamp (in milliseconds since epoch) when the measure began.
	 */
	private long begin;

	/**
	 * The status of the measure.
	 */
	private Status status = Status.PASSED;

	/**
	 * Optional key-value data providing additional context or metrics.
	 */
	private Map<String, Object> data;

	/**
	 * Creates an empty {@code Measure} instance with no field values defined.
	 * Typically used when fields are to be set later.
	 */
	public Measure() {
		super();
	}

	/**
	 * Creates a {@code Measure} instance with basic information and no explicit
	 * status.
	 *
	 * @param name     the name or identifier of the measure
	 * @param duration the duration of the measure in milliseconds
	 * @param begin    the timestamp when the measure began
	 * @param data     additional contextual data
	 */
	public Measure(String name, long duration, long begin, Map<String, Object> data) {
		this(name, duration, begin, data, null);
	}

	/**
	 * Creates a fully specified {@code Measure} instance.
	 *
	 * @param name     the name or identifier of the measure
	 * @param duration the duration of the measure in milliseconds
	 * @param begin    the timestamp when the measure began
	 * @param data     additional contextual data
	 * @param status   the outcome status of the measurement
	 */
	public Measure(String name, long duration, long begin, Map<String, Object> data, Status status) {
		super();
		this.name = name;
		this.duration = duration;
		this.begin = begin;
		this.data = data;
		this.status = status;
	}

	/**
	 * @return the duration of the measure, in milliseconds
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * @param duration the duration to set, in milliseconds
	 */
	public void setDuration(long duration) {
		this.duration = duration;
	}

	/**
	 * @return the start timestamp (in milliseconds since epoch) of the measure
	 */
	public long getBegin() {
		return begin;
	}

	/**
	 * @param begin the start timestamp to set, in milliseconds since epoch
	 */
	public void setBegin(long begin) {
		this.begin = begin;
	}

	/**
	 * @return the name or identifier of the measure
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name or identifier to assign to this measure
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return a map of additional contextual data associated with this measure
	 */
	public Map<String, Object> getData() {
		return data;
	}

	/**
	 * @param data additional contextual data to associate with this measure
	 */
	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	/**
	 * @return the result status of the measure
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * @param status the status to set for this measure
	 */
	public void setStatus(Status status) {
		this.status = status;
	}
}
