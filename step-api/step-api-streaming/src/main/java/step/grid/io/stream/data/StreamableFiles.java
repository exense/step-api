package step.grid.io.stream.data;

import java.io.*;

// Helper class to provide an OutputStream (for appending to an existing file at a given position),
// and an InputStream (for reading a chunk of a given file). Currently used by storage backend on the server
// side, but could potentially also be useful for a standalone download client.
public class StreamableFiles {
    public static OutputStream getOutputStream(File file, long startPosition) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rwd");
        raf.seek(startPosition);
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                raf.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                raf.write(b, off, len);
            }

            @Override
            public void close() throws IOException {
                raf.close();
            }
        };
    }

    public static InputStream getInputStream(File file, long startPosition, long endPosition) throws IOException {
        return new RandomAccessFileChunkInputStream(new RandomAccessFile(file, "r"), startPosition, endPosition);
    }


}
