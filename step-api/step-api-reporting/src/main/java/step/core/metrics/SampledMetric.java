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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Abstract base for sampled metrics (gauge and histogram) that track a distribution
 * of observed values over a reporting interval.
 * <p>
 * Accumulates {@code count}, {@code sum}, {@code min}, {@code max}, a bucketed
 * {@code distribution}, and {@code last} (most recently observed value).
 * All accumulators are thread-safe; {@code count/sum/min/max/distribution} are reset
 * on each {@link #flush()}, while {@code last} is retained across flushes.
 * <p>
 * Use {@link #observe(long)} to record values.
 */
public abstract class SampledMetric extends Metric {

    /**
     * Distribution bucket precision: values are rounded down to the nearest multiple
     * of this value before being placed in a distribution bucket.
     */
    private static final long PCL_PRECISION = 10;

    private final LongAdder countAdder = new LongAdder();
    private final LongAdder sumAdder = new LongAdder();
    private final AtomicLong minAtomic = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxAtomic = new AtomicLong(Long.MIN_VALUE);
    private final ConcurrentHashMap<Long, LongAdder> distributionAccumulator = new ConcurrentHashMap<>();
    private volatile long last;

    protected SampledMetric(String name) {
        super(name);
    }

    protected SampledMetric(String name, Map<String, String> labels) {
        super(name, labels);
    }

    /**
     * Records a single observation, using the current wall-clock time as the observation
     * timestamp for rate-limit decisions.
     *
     * @param value the observed value (e.g. a response time in ms, or a quantity)
     */
    public SampledMetric observe(long value) {
        observe(value, System.currentTimeMillis());
        return this;
    }

    /**
     * Records a single observation, using the supplied timestamp as the observation
     * timestamp for rate-limit decisions.
     *
     * @param value                  the observed value (e.g. a response time in ms, or a quantity)
     * @param observationTimestampMs epoch milliseconds of this observation
     */
    public SampledMetric observe(long value, long observationTimestampMs) {
        countAdder.increment();
        sumAdder.add(value);
        minAtomic.updateAndGet(cur -> Math.min(cur, value));
        maxAtomic.updateAndGet(cur -> Math.max(cur, value));
        distributionAccumulator
                .computeIfAbsent(value - value % PCL_PRECISION, k -> new LongAdder())
                .increment();
        this.last = value;
        notifyObserved(observationTimestampMs);
        return this;
    }

    /**
     * Captures the accumulated distribution statistics into a new {@link MetricSample},
     * resets all accumulators except {@code last}, and returns the snapshot.
     */
    @Override
    public MetricSample flush() {
        long count = countAdder.sumThenReset();
        long sum = sumAdder.sumThenReset();
        long min = count > 0 ? minAtomic.getAndSet(Long.MAX_VALUE) : 0;
        long max = count > 0 ? maxAtomic.getAndSet(Long.MIN_VALUE) : 0;
        Map<Long, Long> distribution = new HashMap<>();
        distributionAccumulator.forEach((k, adder) -> {
            long val = adder.sumThenReset();
            if (val > 0) {
                distribution.put(k, val);
            }
        });
        return new MetricSample(getLastObservedTimestampMs(), getName(), getLabels(), getType(), count, sum, min, max, last, distribution);
    }
}
