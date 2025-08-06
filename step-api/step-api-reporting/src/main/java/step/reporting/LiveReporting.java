package step.reporting;

import step.streaming.client.upload.StreamingUploadProvider;
import step.streaming.client.upload.StreamingUploads;

import java.util.Objects;

public class LiveReporting {

    public final StreamingUploads fileUploads;

    public LiveReporting(StreamingUploads streamingUploads) {
        this.fileUploads = Objects.requireNonNull(streamingUploads);
    }
}
