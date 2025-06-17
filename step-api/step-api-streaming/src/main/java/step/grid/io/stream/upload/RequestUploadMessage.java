package step.grid.io.stream.upload;

// Client -> Server: Initiating new upload, providing file metadata
public class RequestUploadMessage extends UploadProtocolMessage{
    public String filename;
    public String contentMimeType;

    public RequestUploadMessage(String filename, String contentMimeType) {
        this.filename = filename;
        this.contentMimeType = contentMimeType;
    }
    public RequestUploadMessage() {} // for Jackson
}
