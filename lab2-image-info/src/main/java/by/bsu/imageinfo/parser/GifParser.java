package by.bsu.imageinfo.parser;

import by.bsu.imageinfo.ImageInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GifParser {

    private static final boolean LE = true;

    private GifParser() {
    }

    public static ImageInfo parse(BinaryFile f, Path path) throws IOException {
        int width = f.u16(6, LE);
        int height = f.u16(8, LE);
        int packed = f.u8(10);
        boolean globalPalette = (packed & 0x80) != 0;
        int globalBits = (packed & 0x07) + 1;

        Map<String, String> extras = new LinkedHashMap<>();
        Values.put(extras, "Версия GIF", f.text(3, 3));

        Integer depth = null;
        long position = 13;
        if (globalPalette) {
            depth = globalBits;
            Values.put(extras, "Размер палитры", Values.colors(1L << globalBits));
            position += 3L << globalBits;
        }

        int frames = 0;
        while (true) {
            int block = f.u8(position);
            if (block == 0x3B) {
                break;
            }
            if (block == 0x21) {
                position = skipSubBlocks(f, position + 2);
            } else if (block == 0x2C) {
                int flags = f.u8(position + 9);
                position += 10;
                if ((flags & 0x80) != 0) {
                    int localBits = (flags & 0x07) + 1;
                    if (depth == null) {
                        depth = localBits;
                        Values.put(extras, "Размер палитры", Values.colors(1L << localBits) + " (локальная)");
                    }
                    position += 3L << localBits;
                }
                position = skipSubBlocks(f, position + 1);
                frames++;
            } else {
                throw new IOException("Неизвестный блок GIF: " + block);
            }
        }
        Values.put(extras, "Кадров", frames + (frames > 1 ? " (анимация)" : ""));
        return new ImageInfo(path, "GIF", width, height, null, depth, "LZW", "OK", extras);
    }

    private static long skipSubBlocks(BinaryFile f, long position) throws IOException {
        long current = position;
        int length;
        while ((length = f.u8(current)) != 0) {
            current += length + 1;
        }
        return current + 1;
    }
}
