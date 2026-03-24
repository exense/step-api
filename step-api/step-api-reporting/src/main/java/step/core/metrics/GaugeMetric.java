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
 * A gauge metric that tracks a distribution of observed values over each reporting interval.
 * <p>
 * Suitable for quantities that can go up or down (e.g. queue depth, thread count, temperature).
 * Use {@link #observe(long)} to record each measurement.
 * <p>
 * On each flush, the accumulated {@code count/sum/min/max/distribution} are captured and reset.
 * The {@code last} field retains the most recently observed value across flushes.
 */
public class GaugeMetric extends SampledMetric {

    public GaugeMetric(String name) {
        super(name);
    }

    public GaugeMetric(String name, Map<String, String> labels) {
        super(name, labels);
    }

    @Override
    public MetricType getType() {
        return MetricType.GAUGE;
    }
}
