package step.grid.io.stream;

// This is similar to the TransferStatus, maybe the two classes can be combined?
public enum StreamableResourceStatus {
    CREATED,
    UPLOADING,
    FINISHED,
    FAILED,
}
