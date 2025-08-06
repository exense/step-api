package step.reporting;

import step.streaming.client.upload.StreamingUploads;

public class LiveReporting {

    public final StreamingUploads fileUploads;

    public LiveReporting(StreamingUploads fileUploads) {
        this.fileUploads = fileUploads; // attention: could be null in certain circumstances
    }
}
