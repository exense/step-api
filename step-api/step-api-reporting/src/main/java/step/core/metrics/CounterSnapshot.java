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
 * Snapshot produced by {@link CounterMetric#flush()}, carrying the counter's
 * accumulated state for a single reporting interval.
 * <p>
 * {@code accumulatedDiff} — increments recorded since the last flush;
 * maps to {@code Bucket.count} on the controller side, enabling rate calculation.<br>
 * {@code longRunningTotal} — all-time running total, never reset;
 * maps to {@code Bucket.sum}, enabling the frontend to display the absolute total.
 */
public class CounterSnapshot extends MetricSnapshot {

    private long accumulatedDiff;
    private long longRunningTotal;

    /** Required for Jackson deserialization. */
    public CounterSnapshot() {
    }

    public CounterSnapshot(long snapshotTimestamp, String name, Map<String, String> labels,
                           long accumulatedDiff, long longRunningTotal) {
        super(snapshotTimestamp, name, labels, MetricType.COUNTER);
        this.accumulatedDiff = accumulatedDiff;
        this.longRunningTotal = longRunningTotal;
    }

    /** Returns the number of increments recorded since the last flush. */
    public long getAccumulatedDiff() {
        return accumulatedDiff;
    }

    /** Required for Jackson deserialization. */
    public void setAccumulatedDiff(long accumulatedDiff) {
        this.accumulatedDiff = accumulatedDiff;
    }

    /** Returns the all-time running total, never reset between flushes. */
    public long getLongRunningTotal() {
        return longRunningTotal;
    }

    /** Required for Jackson deserialization. */
    public void setLongRunningTotal(long longRunningTotal) {
        this.longRunningTotal = longRunningTotal;
    }
}
