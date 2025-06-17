package step.grid.io.stream.download;

import step.grid.io.stream.StreamableResourceStatus;

// Server -> Client: Status of the resource has changed (upload "stage" status and/or size)
public class ResourceStatusChangedMessage extends DownloadProtocolMessage {
    public StreamableResourceStatus status;
    public Long currentSize;

    // for programmatic construction (Jackson)
    public ResourceStatusChangedMessage() {
    }

    public ResourceStatusChangedMessage(StreamableResourceStatus status, Long currentSize) {
        this.status = status;
        this.currentSize = currentSize;
    }
}
