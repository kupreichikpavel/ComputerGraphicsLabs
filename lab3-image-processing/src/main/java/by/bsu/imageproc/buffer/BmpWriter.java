package by.bsu.imageproc.buffer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BmpWriter {

    private static final int HEADER_SIZE = 54;

    private BmpWriter() {
    }

    public static void write(ColorImage image, Path path) throws IOException {
        int width = image.width();
        int height = image.height();
        int stride = (width * 3 + 3) / 4 * 4;
        int pixelBytes = stride * height;
        byte[] file = new byte[HEADER_SIZE + pixelBytes];

        file[0] = 'B';
        file[1] = 'M';
        putInt(file, 2, file.length);
        putInt(file, 10, HEADER_SIZE);
        putInt(file, 14, 40);
        putInt(file, 18, width);
        putInt(file, 22, height);
        putShort(file, 26, 1);
        putShort(file, 28, 24);
        putInt(file, 34, pixelBytes);
        putInt(file, 38, 3780);
        putInt(file, 42, 3780);

        int[] argb = image.data();
        for (int y = 0; y < height; y++) {
            int row = HEADER_SIZE + (height - 1 - y) * stride;
            for (int x = 0; x < width; x++) {
                int p = argb[y * width + x];
                int o = row + x * 3;
                file[o] = (byte) p;
                file[o + 1] = (byte) (p >> 8);
                file[o + 2] = (byte) (p >> 16);
            }
        }
        try (OutputStream out = Files.newOutputStream(path)) {
            out.write(file);
        }
    }

    private static void putInt(byte[] b, int offset, int value) {
        b[offset] = (byte) value;
        b[offset + 1] = (byte) (value >> 8);
        b[offset + 2] = (byte) (value >> 16);
        b[offset + 3] = (byte) (value >> 24);
    }

    private static void putShort(byte[] b, int offset, int value) {
        b[offset] = (byte) value;
        b[offset + 1] = (byte) (value >> 8);
    }
}
