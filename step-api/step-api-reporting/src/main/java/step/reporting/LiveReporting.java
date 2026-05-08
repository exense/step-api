package step.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.reporting.impl.DelegatingLiveMeasureDestination;
import step.reporting.impl.DelegatingLiveMetricDestination;
import step.reporting.impl.LiveMeasureDestination;
import step.reporting.impl.LiveMetricDestination;
import step.streaming.client.upload.StreamingUploadProvider;
import step.streaming.client.upload.StreamingUploads;
import step.streaming.client.upload.impl.local.DiscardingStreamingUploadProvider;

/**
 * LiveReporting is a container class for real-time reporting features, i.e.,
 * data which is made available to Step users immediately even during Keyword execution,
 * as opposed to only after a Keyword execution is finished.
 * <p>
 * The current implementation supports live-streaming of attachments (i.e., files),
 * as well as measures.
 * <p>
 * API users will get access to an existing {@link LiveReporting} instance and should only
 * interact with its exposed fields. In particular, instantiation and closure of the reporting
 * object is reserved for the framework, please do not invoke the corresponding methods when
 * merely using the functionality.
 */
public class LiveReporting {
    private static final Logger logger = LoggerFactory.getLogger(LiveReporting.class);

    /**
     * {@link StreamingUploads} instance providing functionality for real-time streaming file uploads.
     */
    public final StreamingUploads fileUploads;

    public final LiveMeasures measures;

    /**
     * Live metric reporting channel. Keyword developers register {@link step.core.metrics.Metric}
     * instances here; the framework handles periodic flushing and dispatch to the controller.
     */
    public final LiveMetrics metrics;

    /**
     * Instantiates a new LiveReporting object. <b>Reserved for the framework</b>, do not use unless
     * explicitly instructed to.
     *
     * @param streamingUploadProvider provider instance for creating streaming uploads
     * @param liveMeasureDestination  data sink where measures are forwarded to
     */
    public LiveReporting(StreamingUploadProvider streamingUploadProvider, LiveMeasureDestination liveMeasureDestination) {
        this(streamingUploadProvider, liveMeasureDestination, null);
    }

    /**
     * Instantiates a new LiveReporting object with metric support.
     * <b>Reserved for the framework</b>, do not use unless explicitly instructed to.
     *
     * @param streamingUploadProvider provider instance for creating streaming uploads
     * @param liveMeasureDestination  data sink where measures are forwarded to
     * @param liveMetricDestination   data sink where metric snapshots are forwarded to;
     *                                {@code null} installs a discarding default
     */
    public LiveReporting(StreamingUploadProvider streamingUploadProvider,
                         LiveMeasureDestination liveMeasureDestination,
                         LiveMetricDestination liveMetricDestination) {
        if (streamingUploadProvider == null) {
            // FIXME: improve to give option to save locally -- SED-4192
            logger.debug("LiveReporting initializing without a StreamingUploadProvider object, instantiating one that discards all data");
            streamingUploadProvider = new DiscardingStreamingUploadProvider();
        }
        fileUploads = new StreamingUploads(streamingUploadProvider);

        if (liveMeasureDestination == null) {
            logger.debug("LiveReporting instantiated without a LiveMeasureSink object, instantiating one that discards all data by default");
            liveMeasureDestination = new DelegatingLiveMeasureDestination();
        }
        measures = new LiveMeasures(liveMeasureDestination);

        if (liveMetricDestination == null) {
            logger.debug("LiveReporting instantiated without a LiveMetricDestination, instantiating one that discards all data by default");
            liveMetricDestination = new DelegatingLiveMetricDestination();
        }
        metrics = new LiveMetrics(liveMetricDestination);
    }

    /**
     * Closes the LiveReporting object after use.
     * This performs some cleanup tasks, for instance ongoing uploads which have not been properly finished will
     * be canceled. <b>Reserved for the framework</b>, do not use unless
     * explicitly instructed to.
     */
    public void close() {
        try {
            fileUploads.close();
        } catch (Exception unexpected) {
            // this SHOULD never happen, but just to be safe in case something goes terribly wrong,
            // so we don't break the code which uses us and may not be prepared for exceptions...
            logger.error("Unexpected exception occurred while closing LiveReporting", unexpected);
        }
        try {
            measures.close();
        } catch (Exception unexpected) {
            logger.error("Unexpected exception occurred while closing LiveReporting", unexpected);
        }
        try {
            metrics.close();
        } catch (Exception unexpected) {
            logger.error("Unexpected exception occurred while closing LiveReporting metrics", unexpected);
        }
    }
}
