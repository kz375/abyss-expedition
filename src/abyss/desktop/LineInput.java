package abyss.desktop;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Non-blocking UI submission; only the game thread waits for the next UTF-8 line. */
public final class LineInput extends InputStream {
    private final BlockingQueue<byte[]> lines = new LinkedBlockingQueue<>(64);
    private static final byte[] EOF = new byte[0];
    private byte[] current = EOF;
    private int offset;
    private volatile boolean closed;
    public boolean submit(String text) { return !closed && lines.offer((text + "\n").getBytes(StandardCharsets.UTF_8)); }
    @Override public int read() throws IOException {
        while (offset == current.length) {
            if (closed && lines.isEmpty()) return -1;
            try { current = lines.take(); offset = 0; }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new InterruptedIOException(); }
            if (current == EOF) return -1;
        }
        return current[offset++] & 255;
    }
    @Override public int read(byte[] buffer, int start, int length) throws IOException {
        java.util.Objects.checkFromIndexSize(start, length, buffer.length);
        if (length == 0) return 0;
        int first = read(); if (first < 0) return -1;
        buffer[start] = (byte)first;
        int rest = Math.min(length-1, current.length-offset);
        System.arraycopy(current,offset,buffer,start+1,rest); offset += rest;
        return rest+1;
    }
    @Override public void close() { closed = true; lines.clear(); lines.offer(EOF); }
}
