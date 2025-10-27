package step.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.reports.Measure;
import step.reporting.impl.LiveMeasureDestination;

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
    /**
     * Concrete implementation class where measures are forwarded
     */
    public final LiveMeasureDestination destination;
    // ConcurrentLinkedDeque is a thread-safe alternative to stacks
    private final ConcurrentLinkedDeque<Measure> ongoingStack = new ConcurrentLinkedDeque<>();

    /**
     * Instantiates a new LiveMeasures object.
     * <b>Reserved for the framework</b>, do not use for normal API usage.
     * @param destination data destination object, i.e., where measures are forwarded to
     */
    public LiveMeasures(LiveMeasureDestination destination) {
        this.destination = destination;
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
        Objects.requireNonNull(measure, "measure must not be null");
        Objects.requireNonNull(measure.getStatus(), "measure status must not be null");
        Objects.requireNonNull(measure.getName(), "measure name must not be null");
        destination.accept(measure);
    }

    /**
     * Starts a new measure with the given name and pushes it onto the internal stack.
     * <p>
     * The measure will remain open until a corresponding call to {@link #stopMeasure()}
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
     * Stops the most recently started measure, assigns it a {@code PASSED} status, and submits it.
     *
     * @throws IllegalArgumentException if called without a matching call to {@link #startMeasure(String)}
     * @see #stopMeasure(Measure.Status)
     * @see #stopMeasure(Measure.Status, Map)
     */
    public void stopMeasure() {
        stopMeasure(Measure.Status.PASSED);
    }

    /**
     * Stops the most recently started measure, assigns the given status, and submits it.
     *
     * @param status the status of the measure, must not be null.
     * @throws IllegalArgumentException if called without a matching call to {@link #startMeasure(String)}
     * @see #stopMeasure(Measure.Status, Map)
     */
    public void stopMeasure(Measure.Status status) {
        stopMeasure(status, null);
    }

    /**
     * Stops the most recently started measure, attaches the given data, and submits it.
     * <p>
     * The measure's resulting duration is automatically computed based on its start time.
     *
     * @param status the status of the measure, must not be null.
     * @param data optional key-value data to associate with the measure, may be null.
     * @throws IllegalArgumentException if called without a matching call to {@link #startMeasure(String)}
     */
    public void stopMeasure(Measure.Status status, Map<String, Object> data) {
        long now = System.currentTimeMillis();
        try {
            Measure measure = ongoingStack.pop();
            measure.setDuration(now - measure.getBegin());
            measure.setData(data);
            measure.setStatus(status);
            addMeasure(measure);
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
        destination.close();
    }
}
