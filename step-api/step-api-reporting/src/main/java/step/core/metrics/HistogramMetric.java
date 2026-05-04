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
 * A histogram metric that tracks a distribution of observed values over each reporting interval.
 * <p>
 * Suitable for measuring durations, sizes, or other bounded quantities where the distribution
 * shape (percentiles) is meaningful (e.g. response times, payload sizes).
 * Use {@link #observe(long)} to record each measurement.
 * <p>
 * Structurally identical to {@link GaugeMetric}; the distinction is semantic and determines
 * how handlers (e.g. {@code PrometheusHandler}) expose the metric.
 */
public class HistogramMetric extends SampledMetric {

    public HistogramMetric(String name) {
        super(name);
    }

    public HistogramMetric(String name, Map<String, String> labels) {
        super(name, labels);
    }

    /**
     * @param percentilePrecision see {@link SampledMetric#SampledMetric(String, long)}
     */
    public HistogramMetric(String name, long percentilePrecision) {
        super(name, percentilePrecision);
    }

    /**
     * @param percentilePrecision see {@link SampledMetric#SampledMetric(String, long)}
     */
    public HistogramMetric(String name, Map<String, String> labels, long percentilePrecision) {
        super(name, labels, percentilePrecision);
    }

    @Override
    public InstrumentType getType() {
        return InstrumentType.HISTOGRAM;
    }
}
