package step.grid.io.stream;

// Used for signaling when a transfer (upload) has finished (by being wrapped in a CompletableFuture).
public enum TransferStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED;

    private String details = null;
    private long transferredBytes = 0;

    public String getDetails() {
        return details;
    }

    public TransferStatus setDetails(String details) {
        this.details = details;
        return this;
    }

    public long getTransferredBytes() {
        return transferredBytes;
    }

    public TransferStatus setTransferredBytes(long transferredBytes) {
        this.transferredBytes = transferredBytes;
        return this;
    }


    @Override
    public String toString() {
        return super.toString()+"{details=" + details + ", transferredBytes=" + transferredBytes + '}';
    }
}
