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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/**
 * Collects {@link MetricSample}s produced by registered {@link Metric}s.
 * <p>
 * Analogous to {@link step.core.reports.MeasurementsBuilder} for {@link step.core.reports.Measure}s.
 * <p>
 * When a {@link Metric} is registered via {@link #register(Metric)}, an observation listener is
 * installed on it. Each observation notifies the listener, which rate-limits flushing: a
 * {@link Metric#flush()} is only triggered if the metric has never been flushed before or if at
 * least {@value #FLUSH_INTERVAL_MS} ms have elapsed since the last flush. This prevents excessive
 * sample production for high-frequency metrics while still capturing data promptly.
 * <p>
 * A guaranteed final flush of all registered metrics is performed by {@link #getSamples()} /
 * {@link #close()}, ensuring no accumulated values are lost at the end of a keyword execution.
 * <p>
 * Supports two modes:
 * <ul>
 *   <li><b>Batch mode</b> (no forward consumer): samples are only accumulated internally and
 *       returned via {@link #getSamples()}.</li>
 *   <li><b>Streaming mode</b> (with forward consumer): each sample is also forwarded to the
 *       supplied consumer (e.g. a {@code step.streaming.util.BatchProcessor}) in addition to
 *       being accumulated.</li>
 * </ul>
 */
public class MetricSamplesBuilder implements AutoCloseable {

    public static final long FLUSH_INTERVAL_MS = 5000;

    private final long flushIntervalMs;
    private final Consumer<MetricSample> forwardConsumer;
    private final List<MetricSample> collectedSamples = new ArrayList<>();
    /** Kept for the final flush. */
    private final ConcurrentLinkedQueue<Metric> registeredMetrics = new ConcurrentLinkedQueue<>();

    /**
     * Creates a builder in batch mode with the default {@value #FLUSH_INTERVAL_MS} ms interval.
     */
    public MetricSamplesBuilder() {
        this(FLUSH_INTERVAL_MS, null);
    }

    /**
     * Creates a builder in streaming mode with the default {@value #FLUSH_INTERVAL_MS} ms interval.
     *
     * @param forwardConsumer called synchronously on every new sample; may be {@code null}
     */
    public MetricSamplesBuilder(Consumer<MetricSample> forwardConsumer) {
        this(FLUSH_INTERVAL_MS, forwardConsumer);
    }

    /**
     * Creates a builder with a custom flush interval.
     *
     * @param flushIntervalMs minimum milliseconds between two observation-triggered flushes
     * @param forwardConsumer called synchronously on every new sample; may be {@code null}
     */
    public MetricSamplesBuilder(long flushIntervalMs, Consumer<MetricSample> forwardConsumer) {
        this.flushIntervalMs = flushIntervalMs;
        this.forwardConsumer = forwardConsumer;
    }

    /**
     * Registers a metric with this builder. An observation listener is installed on the metric
     * that rate-limits flushing to at most once per {@link #flushIntervalMs} ms.
     *
     * @param metric the metric to track; must not be {@code null}
     */
    public void register(Metric metric) {
        registeredMetrics.add(metric);
        AtomicLong lastFlushTime = new AtomicLong(0L);
        LongConsumer listener = observationTimestampMs -> {
            long last = lastFlushTime.get();
            if (observationTimestampMs - last >= flushIntervalMs
                    && lastFlushTime.compareAndSet(last, observationTimestampMs)) {
                publish(metric.flush());
            }
        };
        metric.setObservationListener(listener);
    }

    /**
     * Adds pre-existing samples directly to the collected set, bypassing the flush cycle.
     * Useful when merging samples from an already-built output.
     *
     * @param samples the samples to add; ignored if {@code null} or empty
     */
    public void addSamples(List<MetricSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return;
        }
        synchronized (collectedSamples) {
            collectedSamples.addAll(samples);
        }
    }

    /**
     * Performs a final flush of all registered metrics (regardless of the rate limit),
     * appends any non-empty samples to the collected set, and returns the complete list.
     * <p>
     * Calling this more than once is safe but will add duplicate final-flush samples
     * for any metrics that accumulate new observations between calls.
     *
     * @return a snapshot of all collected samples including the final flush; never {@code null}
     */
    public List<MetricSample> getSamples() {
        for (Metric metric : registeredMetrics) {
            MetricSample sample = metric.flush();
            if (sample.getCount() > 0) {
                publish(sample);
            }
        }
        synchronized (collectedSamples) {
            return new ArrayList<>(collectedSamples);
        }
    }

    /**
     * Performs the final flush (same as {@link #getSamples()}) and discards the result.
     * Intended for the streaming use case where the forward consumer handles delivery.
     */
    @Override
    public void close() {
        getSamples();
    }

    private void publish(MetricSample sample) {
        synchronized (collectedSamples) {
            collectedSamples.add(sample);
        }
        if (forwardConsumer != null) {
            forwardConsumer.accept(sample);
        }
    }
}
