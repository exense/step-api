package step.reporting.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.reports.Measure;

public class DiscardingLiveMeasureSink implements LiveMeasureSink {
    private static final Logger logger = LoggerFactory.getLogger(DiscardingLiveMeasureSink.class);

    @Override
    public void accept(Measure measure) {
        logger.warn("Received, but discarding live measure {}, duration={}", measure.getName(), measure.getDuration());
    }

    @Override
    public void close() {
    }
}
