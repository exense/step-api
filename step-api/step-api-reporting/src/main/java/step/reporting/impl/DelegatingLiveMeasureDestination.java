package step.reporting.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.reports.Measure;

public class DelegatingLiveMeasureDestination implements LiveMeasureDestination {
    private static final Logger logger = LoggerFactory.getLogger(DelegatingLiveMeasureDestination.class);

    private LiveMeasureDestination delegate;

    public void setDelegate(LiveMeasureDestination delegate) {
        this.delegate = delegate;
    }

    @Override
    public void accept(Measure measure) {
        if (delegate == null) {
            logger.warn("No delegate set: received, but discarding live measure {}, duration={}", measure.getName(), measure.getDuration());
        } else {
            delegate.accept(measure);
        }
    }

    @Override
    public void close() {
        if (delegate != null) {
            delegate.close();
        }
    }
}
