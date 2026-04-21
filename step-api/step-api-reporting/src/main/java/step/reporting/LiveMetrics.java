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
package step.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.metrics.CounterMetric;
import step.core.metrics.GaugeMetric;
import step.core.metrics.HistogramMetric;
import step.core.metrics.Metric;
import step.reporting.impl.DelegatingLiveMetricDestination;
import step.reporting.impl.LiveMetricDestination;

import java.util.Map;

/**
 * Provides functionality for recording and streaming live metrics.
 * <p>
 * Metrics must be created using one of the registration function to be handled by the live reporting
 */
public class LiveMetrics implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(LiveMetrics.class);

    public final LiveMetricDestination destination;

    /**
     * Creates a {@code LiveMetrics} instance.
     * <b>Reserved for the framework.</b>
     *
     * @param destination sink where flushed metric snapshots are forwarded;
     *                    {@code null} installs a discarding default
     */
    public LiveMetrics(LiveMetricDestination destination) {
        if (destination == null) {
            logger.debug("LiveMetrics instantiated without a destination, discarding all metrics by default");
            destination = new DelegatingLiveMetricDestination();
        }
        this.destination = destination;
    }

    /**
     * Creates and register a counter metric
     * @param name the name of the metric
     * @return the registered CounterMetric
     */
    public CounterMetric registerCounter(String name) {
        CounterMetric counterMetric = new CounterMetric(name);
        destination.accept(counterMetric);
        return counterMetric;
    }

    /**
     * Creates and register a counter metric
     * @param name the name of the metric
     * @param labels labels to be attached to this metric (in addition to its name
     * @return the registered CounterMetric
     */
    public CounterMetric registerCounter(String name, Map<String, String> labels) {
        CounterMetric counterMetric = new CounterMetric(name, labels);
        destination.accept(counterMetric);
        return counterMetric;
    }

    /**
     * Creates and register a gauge metric
     * @param name the name of the metric
     * @return the registered CounterMetric
     */
    public GaugeMetric registerGauge(String name) {
        GaugeMetric gaugeMetric = new GaugeMetric(name);
        destination.accept(gaugeMetric);
        return gaugeMetric;
    }

    /**
     * Creates and register a gauge metric
     * @param name the name of the metric
     * @param labels labels to be attached to this metric (in addition to its name
     * @return the registered CounterMetric
     */
    public GaugeMetric registerGauge(String name, Map<String, String> labels) {
        GaugeMetric gaugeMetric = new GaugeMetric(name, labels);
        destination.accept(gaugeMetric);
        return gaugeMetric;
    }

    /**
     * Creates and register a histogram metric
     * @param name the name of the metric
     * @return the registered CounterMetric
     */
    public HistogramMetric registerHistogram(String name) {
        HistogramMetric histogramMetric = new HistogramMetric(name);
        destination.accept(histogramMetric);
        return histogramMetric;
    }

    /**
     * Creates and register a histogram metric
     * @param name the name of the metric
     * @param labels labels to be attached to this metric (in addition to its name
     * @return the registered CounterMetric
     */
    public HistogramMetric registerHistogram(String name, Map<String, String> labels) {
        HistogramMetric histogramMetric = new HistogramMetric(name, labels);
        destination.accept(histogramMetric);
        return histogramMetric;
    }

    /**
     * Generic method allowing to register a Metric created outside of this class
     *
     * @param metric the metric to register; must not be {@code null}
     */
    public void register(Metric metric) {
        destination.accept(metric);
    }

    /**
     * Closes this {@code LiveMetrics} instance and the underlying destination.
     * <b>Reserved for the framework.</b>
     */
    @Override
    public void close() {
        try {
            destination.close();
        } catch (Exception e) {
            logger.error("Unexpected exception while closing LiveMetrics", e);
        }
    }
}
