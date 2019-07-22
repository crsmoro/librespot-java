package xyz.gianlu.librespot.debug;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Gianlu
 */
public class ThrottledInputStream extends InputStream {
    private static final long SLEEP_DURATION_MS = 30;
    private final InputStream in;
    private final long maxBytesPerSec;
    private final long startTime = System.nanoTime();
    private long bytesRead = 0;
    private long totalSleepTime = 0;

    public ThrottledInputStream(@NotNull InputStream in) {
        this(in, Long.MAX_VALUE);
    }

    public ThrottledInputStream(@NotNull InputStream in, long maxBytesPerSec) {
        if (maxBytesPerSec < 0)
            throw new IllegalArgumentException("maxBytesPerSec shouldn't be negative");

        this.in = in;
        this.maxBytesPerSec = maxBytesPerSec;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public int read() throws IOException {
        throttle();
        int data = in.read();
        if (data != -1) bytesRead++;
        return data;
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        throttle();
        int readLen = in.read(b);
        if (readLen != -1) bytesRead += readLen;
        return readLen;
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        throttle();
        int readLen = in.read(b, off, len);
        if (readLen != -1) bytesRead += readLen;
        return readLen;
    }

    private void throttle() throws IOException {
        while (getBytesPerSec() > maxBytesPerSec) {
            try {
                Thread.sleep(SLEEP_DURATION_MS);
                totalSleepTime += SLEEP_DURATION_MS;
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted" + e.getMessage());
                throw new IOException("Thread interrupted", e);
            }
        }
    }

    public long getTotalBytesRead() {
        return bytesRead;
    }

    /**
     * Return the number of bytes read per second
     */
    public long getBytesPerSec() {
        long elapsed = (System.nanoTime() - startTime) / 1000000000;
        if (elapsed == 0) return bytesRead;
        else return bytesRead / elapsed;
    }

    public long getTotalSleepTime() {
        return totalSleepTime;
    }

    @Override
    public String toString() {
        return "ThrottledInputStream{" + "bytesRead=" + bytesRead + ", maxBytesPerSec=" + maxBytesPerSec
                + ", bytesPerSec=" + getBytesPerSec() + ", totalSleepTimeInSeconds=" + totalSleepTime / 1000 + '}';
    }
}
