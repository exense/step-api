package step.reporting.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.reports.Measure;

public class RestUploadingLiveMeasureSink implements LiveMeasureSink {
    private static final Logger logger = LoggerFactory.getLogger(RestUploadingLiveMeasureSink.class);
    private final String restUrl;

    public RestUploadingLiveMeasureSink(String restUrl) {
        this.restUrl = restUrl;
    }

    @Override
    public void accept(Measure measure) {
        // TODO: implement batching, actually upload
        logger.warn("Received measure: name={}, duration={}, would like to forward to {} ", measure.getName(),  measure.getDuration(), restUrl);
    }

    @Override
    public void close() {

    }
}
