package step.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.streaming.client.upload.StreamingUploadProvider;
import step.streaming.client.upload.StreamingUploads;
import step.streaming.client.upload.impl.local.DiscardingStreamingUploadProvider;

/**
 * LiveReporting is a container class for real-time reporting features.
 * Currently, only uploading of files (which are uploaded, and directly available for download
 * in real time e.g. while a Keyword is still executing) is available, but this is planned to
 * be extended in the future with other real-time functionality.
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

    /**
     * Instantiates a new LiveReporting object. <b>Reserved for the framework</b>, do not use unless
     * explicitly instructed to.
     *
     * @param streamingUploadProvider provider instance for creating streaming uploads
     */
    public LiveReporting(StreamingUploadProvider streamingUploadProvider) {
        if (streamingUploadProvider == null) {
            // FIXME: improve to give option to save locally -- SED-4192
            logger.debug("LiveReporting initializing without a provided StreamingUploads object, instantiating one that discards all data");
            streamingUploadProvider = new DiscardingStreamingUploadProvider();
        }
        fileUploads = new StreamingUploads(streamingUploadProvider);
    }

    /**
     * Closes the LiveReporting object after use.
     * This performs some cleanup tasks, for instance ongoing uploads which have not been properly finished will
     * be canceled. <b>Reserved for the framework</b>, do not use unless
     * explicitly instructed to.
     */
    public void close() {
        fileUploads.close();
    }
}
