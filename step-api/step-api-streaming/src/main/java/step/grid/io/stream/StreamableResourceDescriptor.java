package step.grid.io.stream;

import java.util.Objects;

public class StreamableResourceDescriptor {
    public static final class CommonMimeTypes {
        public static final String TEXT_PLAIN = "text/plain";
        public static final String TEXT_HTML = "text/html";
        public static final String APPLICATION_JSON = "application/json";
        public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
        private CommonMimeTypes() {}
    }

    public String filename;
    public String mimeType;
    public StreamableResourceReference reference;

    public StreamableResourceDescriptor() {} // only for programmatic use

    public StreamableResourceDescriptor(String filename, String mimeType, StreamableResourceReference reference) {
        this.filename = Objects.requireNonNull(filename);
        this.mimeType = Objects.requireNonNull(mimeType); // TODO: validate mime-type
        this.reference = Objects.requireNonNull(reference);
    }
}
