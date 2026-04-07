/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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
package step.core.metrics;

import java.util.Map;

/**
 * Immutable snapshot of a {@link Metric}'s accumulated state at a point in time,
 * produced by {@link Metric#flush()}.
 * <p>
 * {@code MetricSnapshot} is the wire format sent to the controller: it contains only
 * plain serializable fields with no accumulator state. It contains all possible values fields
 * to avoid class hierarchy and custom deserialization (no Jackson dependencies in step-api)
 * <p>
 * Getters are exposed as bean properties for Jackson serialization.
 * Setters are provided for Jackson deserialization on the controller side
 * without requiring Jackson annotations in this module.
 */
public class MetricSample {

    private long sampleTime;
    private String name;
    private Map<String, String> labels;
    private InstrumentType type;
    private long count;
    private long sum;
    private long min;
    private long max;
    private long last;
    private Map<Long, Long> distribution;

    /** Required for Jackson deserialization. */
    public MetricSample() {
    }

    public MetricSample(long sampleTime, String name, Map<String, String> labels, InstrumentType type,
                        long count, long sum, long min, long max, long last,
                        Map<Long, Long> distribution) {
        this.sampleTime = sampleTime;
        this.name = name;
        this.labels = labels;
        this.type = type;
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
        this.last = last;
        this.distribution = distribution;
    }

    /** Return the timestamp of this snapshot **/
    public long getSampleTime() {
        return sampleTime;
    }

    /** Required for Jackson deserialization. */
    public void setSampleTime(long sampleTime) {
        this.sampleTime = sampleTime;
    }

    /** Returns the metric name. */
    public String getName() {
        return name;
    }

    /** Required for Jackson deserialization. */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns the labels attached to this metric. */
    public Map<String, String> getLabels() {
        return labels;
    }

    /** Required for Jackson deserialization. */
    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    /**
     * Returns the metric type. Used as a JSON discriminator so the controller
     * can deserialize the correct subclass without requiring Jackson annotations
     * in this module.
     */
    public InstrumentType getType() {
        return type;
    }

    /** Required for Jackson deserialization. */
    public void setType(InstrumentType type) {
        this.type = type;
    }

    /** Returns the number of observations recorded since the last flush. */
    public long getCount() {
        return count;
    }

    /** Required for Jackson deserialization. */
    public void setCount(long count) {
        this.count = count;
    }

    /** Returns the sum of all observed values since the last flush. */
    public long getSum() {
        return sum;
    }

    /** Required for Jackson deserialization. */
    public void setSum(long sum) {
        this.sum = sum;
    }

    /** Returns the minimum observed value since the last flush. */
    public long getMin() {
        return min;
    }

    /** Required for Jackson deserialization. */
    public void setMin(long min) {
        this.min = min;
    }

    /** Returns the maximum observed value since the last flush. */
    public long getMax() {
        return max;
    }

    /** Required for Jackson deserialization. */
    public void setMax(long max) {
        this.max = max;
    }

    /**
     * Returns the most recently observed value. Not reset on flush —
     * provides a "current value" snapshot useful for gauge semantics.
     */
    public long getLast() {
        return last;
    }

    /** Required for Jackson deserialization. */
    public void setLast(long last) {
        this.last = last;
    }

    /**
     * Returns the bucketed value distribution since the last flush.
     * Keys are bucket boundaries; values are observation counts per bucket.
     * Maps to {@code Bucket.percentileDistribution} on the controller side.
     */
    public Map<Long, Long> getDistribution() {
        return distribution;
    }

    /** Required for Jackson deserialization. */
    public void setDistribution(Map<Long, Long> distribution) {
        this.distribution = distribution;
    }
}
