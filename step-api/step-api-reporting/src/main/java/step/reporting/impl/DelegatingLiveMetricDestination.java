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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.metrics.Metric;

/**
 * A {@link LiveMetricDestination} that forwards to a delegate set at runtime by the framework.
 * <p>
 * When no delegate is configured (e.g. in local/test executions), received metrics are discarded
 * with a warning rather than causing an error.
 */
public class DelegatingLiveMetricDestination implements LiveMetricDestination {

    private static final Logger logger = LoggerFactory.getLogger(DelegatingLiveMetricDestination.class);

    private LiveMetricDestination delegate;

    public void setDelegate(LiveMetricDestination delegate) {
        this.delegate = delegate;
    }

    @Override
    public void accept(Metric metric) {
        if (delegate == null) {
            logger.warn("No delegate set: discarding live metric '{}' (type={})", metric.getName(), metric.getType());
        } else {
            delegate.accept(metric);
        }
    }

    @Override
    public void close() {
        if (delegate != null) {
            delegate.close();
        }
    }
}
