package step.grid.io.stream.upload;

import step.grid.io.stream.StreamableResourceDescriptor;

// Server -> Client: server is ready for data upload, provides resource descriptor
public class ReadyForUploadMessage extends UploadProtocolMessage {
    public StreamableResourceDescriptor attachmentDescriptor;

    public ReadyForUploadMessage() {} // Jackson

    public ReadyForUploadMessage(StreamableResourceDescriptor attachmentDescriptor) {
        this.attachmentDescriptor = attachmentDescriptor;
    }
}
