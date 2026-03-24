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
 * Snapshot produced by {@link GaugeMetric#flush()} or {@link HistogramMetric#flush()},
 * carrying the sampled metric's distribution statistics for one reporting interval.
 * <p>
 * The {@link #getType()} discriminator distinguishes gauge from histogram on the controller side.
 * All accumulator fields ({@code count}, {@code sum}, {@code min}, {@code max}, {@code distribution})
 * are reset on each flush. The {@code last} field retains the most recently observed value
 * across flushes, providing a "current value" snapshot useful for gauge semantics.
 */
public class SampledSnapshot extends MetricSnapshot {

    private long count;
    private long sum;
    private long min;
    private long max;
    private long last;
    private Map<Long, Long> distribution;

    /** Required for Jackson deserialization. */
    public SampledSnapshot() {
    }

    public SampledSnapshot(long snapshotTimestamp, String name, Map<String, String> labels, MetricType type,
                           long count, long sum, long min, long max, long last,
                           Map<Long, Long> distribution) {
        super(snapshotTimestamp, name, labels, type);
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
        this.last = last;
        this.distribution = distribution;
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
