package step.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.reports.Measure;
import step.reporting.impl.LiveMeasureSink;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LiveMeasures {
    private final Logger logger = LoggerFactory.getLogger(LiveMeasures.class);
    private final LiveMeasureSink sink;
    // ConcurrentLinkedDeque is a thread-safe alternative to stacks
    private final ConcurrentLinkedDeque<Measure> ongoingStack = new ConcurrentLinkedDeque<>();

    public LiveMeasures(LiveMeasureSink sink) {
        this.sink = sink;
    }

    public void addMeasure(Measure measure) {
        sink.accept(measure);
    }

    public void startMeasure(String measureName) {
        Measure measure = new Measure();
        measure.setName(measureName);
        measure.setBegin(System.currentTimeMillis());
        ongoingStack.push(measure);
    }

    public void stopMeasure() {
        stopMeasure(null);
    }

    public void stopMeasure(Map<String, Object> data) {
        long now = System.currentTimeMillis();
        Measure measure = ongoingStack.pop();
        measure.setDuration(now - measure.getBegin());
        measure.setData(data);
        sink.accept(measure);
    }

    public void close() {
        if (!ongoingStack.isEmpty()) {
            logger.warn("LiveMeasures object closing, but there are still {} ongoing measures; these will be discarded", ongoingStack.size());
        }
        sink.close();
    }
}
