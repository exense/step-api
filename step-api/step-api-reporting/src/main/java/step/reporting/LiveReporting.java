package step.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.streaming.client.upload.StreamingUploadProvider;
import step.streaming.client.upload.StreamingUploads;
import step.streaming.client.upload.impl.local.DiscardingStreamingUploadProvider;
import step.streaming.client.upload.impl.local.LocalDirectoryBackedStreamingUploadProvider;

import java.io.File;

public class LiveReporting {
    public static class ConfigPropertyKeys {
        // The CLI uses the same name for its configuration, this is why it's kept simple
        public static final String STREAMING_UPLOADS_DIRECTORY = "streamingUploadsDir";
    }

    private static final Logger logger = LoggerFactory.getLogger(LiveReporting.class);

    public final StreamingUploads fileUploads;

    public LiveReporting(StreamingUploadProvider streamingUploadProvider) {
        if (streamingUploadProvider == null) {
            logger.debug("LiveReporting initializing without a provided StreamingUploads object, locally instantiating one");
            streamingUploadProvider = instantiateStreamingUploadProvider();
        }
        fileUploads = new StreamingUploads(streamingUploadProvider);
    }

    private StreamingUploadProvider instantiateStreamingUploadProvider() {
        String directoryProperty = System.getProperty(ConfigPropertyKeys.STREAMING_UPLOADS_DIRECTORY, null);
        if (directoryProperty == null) {
            logger.info("No configuration found for streaming upload directory, discarding all uploaded data");
            logger.info("If you want to store uploads locally, set the property '{}' to an existing directory", ConfigPropertyKeys.STREAMING_UPLOADS_DIRECTORY);
            return new DiscardingStreamingUploadProvider();
        } else {
            logger.info("Saving streaming uploads to directory configured by property '{}': {}", ConfigPropertyKeys.STREAMING_UPLOADS_DIRECTORY, directoryProperty);
            return new LocalDirectoryBackedStreamingUploadProvider(new File(directoryProperty));
        }
    }
}
