package by.bsu.imageinfo.parser;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class BinaryFile implements AutoCloseable {

    private static final int HEAD_SIZE = 64 * 1024;
    private static final int WINDOW_SIZE = 16 * 1024;

    private final FileChannel channel;
    private final long size;
    private final byte[] head;
    private final byte[] window = new byte[WINDOW_SIZE];
    private long windowStart = -1;
    private int windowLength;

    private BinaryFile(FileChannel channel) throws IOException {
        this.channel = channel;
        this.size = channel.size();
        this.head = new byte[(int) Math.min(size, HEAD_SIZE)];
        readDirect(0, head, head.length);
    }

    public static BinaryFile open(Path path) throws IOException {
        FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
        try {
            return new BinaryFile(channel);
        } catch (IOException e) {
            channel.close();
            throw e;
        }
    }

    public long size() {
        return size;
    }

    public int u8(long position) throws IOException {
        if (position < 0 || position >= size) {
            throw new EOFException("Чтение за концом файла: смещение " + position + ", размер " + size);
        }
        if (position < head.length) {
            return head[(int) position] & 0xFF;
        }
        if (position < windowStart || position >= windowStart + windowLength) {
            windowLength = (int) Math.min(WINDOW_SIZE, size - position);
            readDirect(position, window, windowLength);
            windowStart = position;
        }
        return window[(int) (position - windowStart)] & 0xFF;
    }

    public int u16(long position, boolean littleEndian) throws IOException {
        int a = u8(position);
        int b = u8(position + 1);
        return littleEndian ? a | (b << 8) : (a << 8) | b;
    }

    public long u32(long position, boolean littleEndian) throws IOException {
        long a = u16(position, littleEndian);
        long b = u16(position + 2, littleEndian);
        return littleEndian ? a | (b << 16) : (a << 16) | b;
    }

    public int s32(long position, boolean littleEndian) throws IOException {
        return (int) u32(position, littleEndian);
    }

    public byte[] bytes(long position, int length) throws IOException {
        if (position < 0 || position + length > size) {
            throw new EOFException("Чтение за концом файла: смещение " + position + ", размер " + size);
        }
        byte[] result = new byte[length];
        if (position + length <= head.length) {
            System.arraycopy(head, (int) position, result, 0, length);
        } else {
            readDirect(position, result, length);
        }
        return result;
    }

    public boolean matches(long position, String ascii) throws IOException {
        if (position + ascii.length() > size) {
            return false;
        }
        for (int i = 0; i < ascii.length(); i++) {
            if (u8(position + i) != ascii.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public String text(long position, int maxLength) throws IOException {
        byte[] bytes = new byte[(int) Math.max(0, Math.min(maxLength, size - position))];
        int length = 0;
        while (length < bytes.length) {
            int c = u8(position + length);
            if (c == 0) {
                break;
            }
            bytes[length++] = (byte) c;
        }
        return new String(bytes, 0, length, StandardCharsets.ISO_8859_1).trim();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    private void readDirect(long position, byte[] target, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(target, 0, length);
        long current = position;
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer, current);
            if (read < 0) {
                throw new EOFException("Файл закончился раньше ожидаемого");
            }
            current += read;
        }
    }
}
