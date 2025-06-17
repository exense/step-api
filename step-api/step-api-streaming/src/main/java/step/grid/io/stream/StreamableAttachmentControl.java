package step.grid.io.stream;

import java.util.concurrent.CompletableFuture;

// This is returned by StreamableAttachmentsHandler when uploading an attachment (e.g. during KW call),
// and contains information about the attachment (descriptor, status), as well
// as the possibility to signal that the upload is finished.
public class StreamableAttachmentControl {
    public final StreamableResourceDescriptor attachmentDescriptor;
    public final CompletableFuture<TransferStatus> transferStatus;
    private final StreamInputFinishedSignal finishedSignal;


    public StreamableAttachmentControl(StreamableResourceDescriptor attachmentDescriptor,
                                       StreamInputFinishedSignal finishedSignal,
                                       CompletableFuture<TransferStatus> transferStatus) {
        this.attachmentDescriptor = attachmentDescriptor;
        this.finishedSignal = finishedSignal;
        this.transferStatus = transferStatus;
    }

    public void finishNormally() {
        finishedSignal.complete(null);
    }

    public void finishWithError(String errorMessage) {
        // this could also be done with .completeExceptionally and adjusting the implementation accordingly
        finishedSignal.complete(errorMessage);
    }

    public boolean isFinished() {
        return finishedSignal.isDone();
    }

}
