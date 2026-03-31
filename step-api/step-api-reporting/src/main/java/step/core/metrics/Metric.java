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
import java.util.function.LongConsumer;

/**
 * Abstract base for all metric types (counter, gauge, histogram).
 * <p>
 * A {@code Metric} accumulates observations in thread-safe internal fields.
 * Each observation fires a lightweight notification to an {@link #setObservationListener(LongConsumer)
 * observation listener}. The listener (installed by {@link MetricSamplesBuilder}) decides whether
 * it is time to call {@link #flush()} based on the elapsed time since the last flush, making
 * sample production rate-limited rather than one-per-observation.
 * <p>
 * Keyword developers interact only with the concrete subclass API
 * (e.g. {@link CounterMetric#increment()}, {@link SampledMetric#observe(long)}).
 * Listener registration and flushing are handled by the framework via {@link MetricSamplesBuilder}.
 */
public abstract class Metric {

    private final String name;
    private final Map<String, String> labels;
    private volatile LongConsumer observationListener;
    private volatile long lastObservedTimestampMs;

    protected Metric(String name) {
        this.name = name;
        this.labels = new HashMap<>();
    }

    protected Metric(String name, Map<String, String> labels) {
        this.name = name;
        this.labels = labels;
    }

    /** Returns the concrete type of this metric. */
    public abstract MetricType getType();

    /**
     * Atomically captures the current accumulated state into a new {@link MetricSample},
     * resets the accumulators, and returns the snapshot.
     * <p>
     * <b>Reserved for the framework.</b> Keyword developers should not call this directly.
     */
    public abstract MetricSample flush();

    /**
     * Sets the listener that is notified after each observation. The listener decides
     * whether to call {@link #flush()} based on its own rate-limiting logic.
     * <b>Reserved for the framework</b> — called by {@link MetricSamplesBuilder#register(Metric)}.
     *
     * @param listener the listener to notify on each observation; receives the observation
     *                 timestamp in epoch milliseconds; {@code null} disables notifications
     */
    public void setObservationListener(LongConsumer listener) {
        this.observationListener = listener;
    }

    /**
     * Called by subclasses after each observation to notify the registered listener, if any,
     * passing the given observation timestamp (epoch milliseconds) for rate-limit decisions.
     *
     * @param observationTimestampMs the timestamp of the observation in epoch milliseconds;
     *                               used by {@link MetricSamplesBuilder} to decide whether to flush
     */
    protected void notifyObserved(long observationTimestampMs) {
        lastObservedTimestampMs = observationTimestampMs;
        LongConsumer l = observationListener;
        if (l != null) {
            l.accept(observationTimestampMs);
        }
    }

    /**
     * Returns the timestamp (epoch milliseconds) of the most recent observation, as passed to
     * {@link #notifyObserved(long)}. Used by {@link #flush()} implementations to stamp the
     * produced {@link MetricSample} with the data-source time rather than the wall-clock flush time.
     * Falls back to the current wall-clock time if no observation has been recorded yet.
     */
    protected long getLastObservedTimestampMs() {
        return lastObservedTimestampMs != 0 ? lastObservedTimestampMs : System.currentTimeMillis();
    }

    /** Returns the metric name. */
    public String getName() {
        return name;
    }

    /**
     * Returns the label map attached to this metric. Labels are forwarded end-to-end
     * to the controller, enabling per-label filtering and aggregation in dashboards.
     */
    public Map<String, String> getLabels() {
        return labels;
    }
}
