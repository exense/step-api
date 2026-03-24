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

/**
 * Abstract base for all metric types (counter, gauge, histogram).
 * <p>
 * A {@code Metric} accumulates observations in thread-safe internal fields.
 * The accumulated state is periodically captured into a {@link MetricSnapshot} via
 * {@link #flush()}, which also resets the accumulators for the next interval.
 * Flushing is triggered by the {@link step.reporting.LiveMetrics} destination
 * implementation (e.g. {@code RestUploadingLiveMetricDestination}) at a configured interval,
 * or at keyword end via {@code OutputBuilder}.
 * <p>
 * Keyword developers interact only with the concrete subclass API
 * (e.g. {@link CounterMetric#increment()}, {@link SampledMetric#observe(long)}).
 * Registration and flushing are handled by the framework.
 */
public abstract class Metric {

    private final String name;
    private final Map<String, String> labels;

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
     * Atomically captures the current accumulated state into a new {@link MetricSnapshot},
     * resets the accumulators for the next interval, and returns the snapshot.
     * <p>
     * <b>Reserved for the framework.</b> Keyword developers should not call this directly.
     */
    public abstract MetricSnapshot flush();

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
