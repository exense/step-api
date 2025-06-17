package step.grid.io.stream;

import step.grid.io.stream.data.LiveFileInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

// For now, I deliberately only added the possibility to add "live" files, where users will need to explicitly signal
// that the file was finished writing. We could easily add another method like addFromInputStream, but the risk
// is that users will manually provide a FileInputStream, and that will prematurely signal an EOF as soon as the
// end of the file is hit (even if it may still grow afterward).
public abstract class StreamableAttachmentsHandler {
    public static final long DEFAULT_POLL_INTERVAL_MS = 200;
    public StreamableAttachmentControl addFromLiveFile(String filename, String contentType, File liveFile) throws IOException {
        return addFromLiveFile(filename, contentType, liveFile, DEFAULT_POLL_INTERVAL_MS, null);
    }

    public StreamableAttachmentControl addFromLiveFile(String filename, String contentMimeType, File liveFile, long liveFilePollIntervalMs, Consumer<Long> uploadedCountCallback) throws IOException {
        // TODO: there's currently no real way to abort an upload except by signaling an error on this signal (or potentially by shutting down the environment). This may need to be improved.
        StreamInputFinishedSignal finishedSignal = new StreamInputFinishedSignal();
        LiveFileInputStream stream = new LiveFileInputStream(liveFile, finishedSignal, liveFilePollIntervalMs);
        CompletableFuture<TransferStatus> transferStatus = new CompletableFuture<>();
        StreamableResourceDescriptor attachment = handleAttachmentCreation(filename, contentMimeType, stream, transferStatus, uploadedCountCallback);
        return new StreamableAttachmentControl(attachment, finishedSignal, transferStatus);
    }

    protected abstract StreamableResourceDescriptor handleAttachmentCreation(String filename, String contentMimeType, InputStream stream, CompletableFuture<TransferStatus> transferStatus, Consumer<Long> uploadCountCallback) throws IOException;
}
