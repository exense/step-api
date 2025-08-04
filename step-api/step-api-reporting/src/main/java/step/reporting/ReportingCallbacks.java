package step.reporting;

import step.streaming.client.upload.StreamingUploadProvider;

public class ReportingCallbacks {

    public final StreamingUploadProvider streamingUploadProvider;

    public ReportingCallbacks(StreamingUploadProvider streamingUploadProvider) {
        this.streamingUploadProvider = streamingUploadProvider;
    }
}
