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
import java.util.concurrent.atomic.AtomicLong;

/**
 * A monotonically increasing counter metric.
 * <p>
 * Tracks two values:
 * <ul>
 *   <li><b>accumulatedDiff</b> — increments recorded since the last {@link #flush()},
 *       enabling rate calculation over the reporting interval.</li>
 *   <li><b>longRunningTotal</b> — all-time running total, never reset,
 *       enabling the frontend to display the absolute count.</li>
 * </ul>
 * Use {@link #increment()} or {@link #increment(long)} to record values.
 * Flushing is handled by the framework; call {@link step.reporting.LiveMetrics#registerCounter}
 * to register this metric for live reporting, or pass it to
 * {@code OutputBuilder.addMetric} for end-of-keyword reporting.
 */
public class CounterMetric extends Metric {

    private final AtomicLong diffAccumulator = new AtomicLong(0);
    private final AtomicLong totalAccumulator = new AtomicLong(0);

    public CounterMetric(String name) {
        super(name);
    }

    public CounterMetric(String name, Map<String, String> labels) {
        super(name, labels);
    }

    @Override
    public MetricType getType() {
        return MetricType.COUNTER;
    }

    /** Increments the counter by 1. */
    public void increment() {
        increment(1);
    }

    /**
     * Increments the counter by {@code amount}.
     *
     * @param amount positive increment value
     */
    public void increment(long amount) {
        diffAccumulator.addAndGet(amount);
        totalAccumulator.addAndGet(amount);
    }

    /**
     * Captures the accumulated diff (then resets it to zero) and the current running total
     * into a new {@link CounterSnapshot} and returns it.
     */
    @Override
    public CounterSnapshot flush() {
        long diff = diffAccumulator.getAndSet(0);
        long total = totalAccumulator.get();
        return new CounterSnapshot(System.currentTimeMillis(), getName(), getLabels(), diff, total);
    }
}
