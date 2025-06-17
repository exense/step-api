package step.grid.io.stream.data;

import step.grid.io.stream.StreamInputFinishedSignal;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

// Provides an InputStream over a file that is (potentially) still growing.
// This stream will behave like a normal InputStream, except that
// it will NOT signal EOF when the end of the file is reached.
// Instead, it will continue to block, waiting for the file to grow,
// and only signal EOF when both the end of the file was reached, and
// the given finishedSignal indicates success. If the finishedSignal indicates
// an error, an IOException is thrown instead.
public class LiveFileInputStream extends InputStream {
    private final RandomAccessFile raf;
    private final StreamInputFinishedSignal finishedSignal;
    private final long pollIntervalMillis;
    private boolean isDone = false;

    public LiveFileInputStream(File file, StreamInputFinishedSignal finishedSignal, long pollIntervalMillis) {
        try {
            this.raf = new RandomAccessFile(Objects.requireNonNull(file), "r");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.finishedSignal = Objects.requireNonNull(finishedSignal);
        this.pollIntervalMillis = pollIntervalMillis;
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        int result = read(b, 0, 1);
        return result == -1 ? -1 : (b[0] & 0xFF);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        while (true) {
            long currentPointer = raf.getFilePointer();
            long fileLength = raf.length();

            if (fileLength > currentPointer) {
                return raf.read(b, off, len);
            }

            if (!isDone) {
                try {
                    // Wait up to pollIntervalMillis for the finished signal
                    String error = finishedSignal.get(pollIntervalMillis, TimeUnit.MILLISECONDS);
                    if (error != null) {
                        // throw exception with user-defined message
                        throw new IOException(error);
                    }
                    isDone = true;
                } catch (java.util.concurrent.TimeoutException e) {
                    // Future not done yet — this is expected, continue to next loop iteration
                } catch (IOException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IOException("Unexpected error: Failed while waiting for completion signal", e);
                }
            }

            if (isDone) {
                return -1; // End of file
            }
        }
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
}
