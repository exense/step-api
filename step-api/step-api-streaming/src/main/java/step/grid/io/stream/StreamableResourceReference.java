package step.grid.io.stream;

// Not sure if this is the final implementation...
public class StreamableResourceReference {
    public String identifier;
    public String uploadEndpoint;
    public String downloadEndpoint;

    public StreamableResourceReference() {}
    public StreamableResourceReference(String identifier, String uploadEndpoint, String downloadEndpoint) {
        this.identifier = identifier;
        this.uploadEndpoint = uploadEndpoint;
        this.downloadEndpoint = downloadEndpoint;
    }
}
