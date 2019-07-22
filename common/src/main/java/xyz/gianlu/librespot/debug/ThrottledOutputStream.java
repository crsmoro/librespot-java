package xyz.gianlu.librespot.debug;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Gianlu
 */
public class ThrottledOutputStream extends OutputStream {
    private static final long SLEEP_DURATION_MS = 30;
    private final long maxBytesPerSecond;
    private final long startTime = System.nanoTime();
    private OutputStream out;
    private long bytesWrite = 0;
    private long totalSleepTime = 0;

    public ThrottledOutputStream(@NotNull OutputStream out) {
        this(out, Long.MAX_VALUE);
    }

    public ThrottledOutputStream(@NotNull OutputStream out, long maxBytesPerSecond) {
        if (maxBytesPerSecond <= 0)
            throw new IllegalArgumentException("maxBytesPerSecond should be greater than zero");

        this.out = out;
        this.maxBytesPerSecond = maxBytesPerSecond;
    }

    @Override
    public void write(int arg0) throws IOException {
        throttle();
        out.write(arg0);
        bytesWrite++;
    }

    @Override
    public void write(@NotNull byte[] b, int off, int len) throws IOException {
        if (len < maxBytesPerSecond) {
            throttle();
            bytesWrite = bytesWrite + len;
            out.write(b, off, len);
            return;
        }

        long currentOffSet = off;
        long remainingBytesToWrite = len;

        do {
            throttle();
            remainingBytesToWrite = remainingBytesToWrite - maxBytesPerSecond;
            bytesWrite = bytesWrite + maxBytesPerSecond;
            out.write(b, (int) currentOffSet, (int) maxBytesPerSecond);
            currentOffSet = currentOffSet + maxBytesPerSecond;
        } while (remainingBytesToWrite > maxBytesPerSecond);

        throttle();
        bytesWrite = bytesWrite + remainingBytesToWrite;
        out.write(b, (int) currentOffSet, (int) remainingBytesToWrite);
    }

    @Override
    public void write(@NotNull byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    private void throttle() throws IOException {
        while (getBytesPerSec() > maxBytesPerSecond) {
            try {
                Thread.sleep(SLEEP_DURATION_MS);
                totalSleepTime += SLEEP_DURATION_MS;
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted" + e.getMessage());
                throw new IOException("Thread interrupted", e);
            }
        }
    }

    /**
     * Return the number of bytes read per second
     */
    public long getBytesPerSec() {
        long elapsed = (System.nanoTime() - startTime) / 1000000000;
        if (elapsed == 0) {
            return bytesWrite;
        } else {
            return bytesWrite / elapsed;
        }
    }

    @Override
    public String toString() {
        return "ThrottledOutputStream{" + "bytesWrite=" + bytesWrite + ", maxBytesPerSecond=" + maxBytesPerSecond
                + ", bytesPerSec=" + getBytesPerSec() + ", totalSleepTimeInSeconds=" + totalSleepTime / 1000 + '}';
    }

    public void close() throws IOException {
        out.close();
    }
}
