package step.grid.io.stream;

import java.io.IOException;

// actual implementation is in step-grid-agent
public interface StreamableResourcesUploadClientFactory {
    StreamableResourcesUploadClient createClient() throws IOException;
}
