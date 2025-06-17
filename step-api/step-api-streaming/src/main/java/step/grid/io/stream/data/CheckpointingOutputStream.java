package step.grid.io.stream.data;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Consumer;

// An OutputStream that will periodically flush its output (e.g. to force
// a transmission via Websocket stream even if the buffer isn't filled yet),
// and that (optionally) notifies a provided listener with the current number
// of bytes written on each checkpoint (used on the server side to periodically
// update the state, even while data is still incoming).
public class CheckpointingOutputStream extends OutputStream {
    public static final long DEFAULT_FLUSH_INTERVAL_MILLIS = 1000;
    private final OutputStream delegate;
    private final long flushIntervalMillis;
    private final Consumer<Long> flushListener; // optional
    private long lastFlushTime = 0; // ← Ensures immediate flush on first write
    private long totalBytesWritten = 0;

    public CheckpointingOutputStream(OutputStream delegate, long flushIntervalMillis, Consumer<Long> flushListener) {
        this.delegate = Objects.requireNonNull(delegate);
        if (flushIntervalMillis <= 0) {
            throw new IllegalArgumentException("flushIntervalMillis must be greater than zero");
        }
        this.flushIntervalMillis = flushIntervalMillis;
        this.flushListener = flushListener;
    }

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
        totalBytesWritten++;
        maybeFlush();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        delegate.write(b, off, len);
        totalBytesWritten += len;
        maybeFlush();
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
        if (flushListener != null) {
            flushListener.accept(totalBytesWritten);
        }
        lastFlushTime = System.currentTimeMillis();
    }

    @Override
    public void close() throws IOException {
        flush(); // final flush and notify
        delegate.close();
    }

    private void maybeFlush() throws IOException {
        long now = System.currentTimeMillis();
        if (now - lastFlushTime >= flushIntervalMillis) {
            flush();
        }
    }

    public long getTotalBytesWritten() {
        return totalBytesWritten;
    }
}
