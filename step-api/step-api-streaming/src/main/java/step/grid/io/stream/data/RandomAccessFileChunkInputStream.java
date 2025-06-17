package step.grid.io.stream.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

// Provides an InputStream over a chunk of a given RandomAccessFile.
public class RandomAccessFileChunkInputStream extends InputStream {
    private final RandomAccessFile raf;
    private final long end;
    private long position;

    public RandomAccessFileChunkInputStream(RandomAccessFile raf, long start, long end) throws IOException {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid start/end positions");
        }
        // TODO: more thorough checks that start/end are within limits of the file?
        this.raf = raf;
        this.end = end;
        this.position = start;
        raf.seek(start);
    }

    @Override
    public int read() throws IOException {
        if (position >= end) return -1;
        int b = raf.read();
        if (b != -1) position++;
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (position >= end) return -1;

        long remaining = end - position;
        int toRead = (int) Math.min(len, remaining);
        int bytesRead = raf.read(b, off, toRead);

        if (bytesRead > 0) {
            position += bytesRead;
        }

        return bytesRead;
    }

    @Override
    public long skip(long n) throws IOException {
        long remaining = end - position;
        long toSkip = Math.min(n, remaining);
        position += toSkip;
        raf.seek(position);
        return toSkip;
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(Integer.MAX_VALUE, end - position);
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
}
