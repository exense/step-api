package step.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.reports.Measure;
import step.reporting.impl.LiveMeasureSink;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;


/**
 * Provides functionality for recording and streaming live performance measures.
 * <p>
 * Measures can be started and stopped in a stack-like fashion, allowing for nested
 * measurements.
 */
public class LiveMeasures {
    private final Logger logger = LoggerFactory.getLogger(LiveMeasures.class);
    private final LiveMeasureSink sink;
    // ConcurrentLinkedDeque is a thread-safe alternative to stacks
    private final ConcurrentLinkedDeque<Measure> ongoingStack = new ConcurrentLinkedDeque<>();

    /**
     * Instantiates a new LiveMeasures object.
     * <b>Reserved for the framework</b>, do not use for normal API usage.
     * @param sink data sink object, i.e., where measures are forwarded to
     */
    public LiveMeasures(LiveMeasureSink sink) {
        this.sink = sink;
    }


    /**
     * Immediately submits a pre-constructed {@link Measure}.
     * <p>
     * This method is typically used when a measure has been created and populated
     * externally rather than through {@link #startMeasure(String)} and {@link #stopMeasure()}.
     * </p>
     *
     * @param measure the measure to submit; must not be {@code null}
     */
    public void addMeasure(Measure measure) {
        sink.accept(Objects.requireNonNull(measure));
    }

    /**
     * Starts a new measure with the given name and pushes it onto the internal stack.
     * <p>
     * The measure will remain open until a corresponding call to {@link #stopMeasure()}
     * or {@link #stopMeasure(Map)} is made.
     * </p>
     *
     * @param measureName the name of the measure to start; must not be {@code null}
     */
    public void startMeasure(String measureName) {
        Measure measure = new Measure();
        measure.setName(Objects.requireNonNull(measureName));
        measure.setBegin(System.currentTimeMillis());
        ongoingStack.push(measure);
    }

    /**
     * Stops the most recently started measure and submits it.
     *
     * @throws IllegalArgumentException if called without a matching call to {@link #startMeasure(String)}
     */
    public void stopMeasure() {
        stopMeasure(null);
    }

    /**
     * Stops the most recently started measure, attaches the given data, and submits it.
     * <p>
     * The measure's resulting duration is automatically computed based on its start time.
     *
     * @param data optional key-value data to associate with the measure
     * @throws IllegalArgumentException if called without a matching call to {@link #startMeasure(String)}
     */
    public void stopMeasure(Map<String, Object> data) {
        long now = System.currentTimeMillis();
        try {
            Measure measure = ongoingStack.pop();
            measure.setDuration(now - measure.getBegin());
            measure.setData(data);
            sink.accept(measure);
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("Unbalanced measures stack: stopMeasure() called but no measure present; did you forget to call startMeasure()?");
        }
    }

    /**
     * Closes this {@code LiveMeasures} instance.
     * <b>Reserved for the framework</b>, do not use for normal API usage.
     */
    public void close() {
        if (!ongoingStack.isEmpty()) {
            logger.warn("LiveMeasures object closing, but there are still {} ongoing measures; these will be discarded", ongoingStack.size());
        }
        sink.close();
    }
}
