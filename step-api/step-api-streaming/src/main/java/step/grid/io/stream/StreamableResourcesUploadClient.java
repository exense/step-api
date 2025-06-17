package step.grid.io.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

// actual implementation is in step-grid-agent
public interface StreamableResourcesUploadClient {
    StreamableResourceDescriptor startUpload(String filename, String contentMimeType, InputStream stream, CompletableFuture<TransferStatus> transferStatus, Consumer<Long> uploadCountCallback) throws IOException;
}
