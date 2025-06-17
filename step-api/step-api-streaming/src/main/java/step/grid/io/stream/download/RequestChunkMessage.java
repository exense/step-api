package step.grid.io.stream.download;

// Client -> Server: request a chunk of a streamable file.
public class RequestChunkMessage extends DownloadProtocolMessage {
    public long startOffset; // inclusive, i.e., start from this offset
    public long endOffset; // exclusive, i.e. end before this offset

    // for programmatic construction (Jackson)
    public RequestChunkMessage() {
    }

    public RequestChunkMessage(long startOffset, long endOffset) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }
}
