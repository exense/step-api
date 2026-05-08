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
package step.reporting.impl;

import step.core.metrics.Metric;

/**
 * Sink for live metric data produced by {@link step.reporting.LiveMetrics}.
 * <p>
 * Each call to {@code accept} registers a live {@link Metric} object with this destination.
 * The destination is responsible for periodically calling {@link Metric#flush()} to capture
 * accumulated state and forwarding the resulting snapshots to the controller (e.g. via REST).
 */
public interface LiveMetricDestination {

    /**
     * Registers a live metric with this destination. The destination stores the metric
     * and calls {@link Metric#flush()} on its own schedule to collect and dispatch snapshots.
     *
     * @param metric the live metric to track; must not be {@code null}
     */
    void accept(Metric metric);

    /**
     * Releases any resources held by this destination (schedulers, HTTP clients, etc.).
     * Called by the framework when the keyword execution ends.
     */
    default void close() {
    }
}
