package by.bsu.imageinfo.parser;

import by.bsu.imageinfo.ImageInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PcxParser {

    private static final boolean LE = true;
    private static final int VGA_PALETTE_SIZE = 769;

    private PcxParser() {
    }

    public static ImageInfo parse(BinaryFile f, Path path) throws IOException {
        int version = f.u8(1);
        int encoding = f.u8(2);
        int bitsPerPixel = f.u8(3);
        int xMin = f.u16(4, LE);
        int yMin = f.u16(6, LE);
        int xMax = f.u16(8, LE);
        int yMax = f.u16(10, LE);
        int horizontalDpi = f.u16(12, LE);
        int planes = f.u8(65);
        if (xMax < xMin || yMax < yMin || planes == 0) {
            throw new IOException("Некорректный заголовок PCX");
        }

        Map<String, String> extras = new LinkedHashMap<>();
        if (bitsPerPixel == 8 && planes == 1 && version == 5) {
            long marker = f.size() - VGA_PALETTE_SIZE;
            if (marker < 128 || f.u8(marker) != 0x0C) {
                throw new IOException("В конце PCX нет 256-цветной палитры — файл обрезан");
            }
            Values.put(extras, "Размер палитры", Values.colors(256));
        }
        Values.put(extras, "Цветовых плоскостей", String.valueOf(planes));
        Values.put(extras, "Версия PCX", String.valueOf(version));

        return new ImageInfo(path, "PCX", xMax - xMin + 1, yMax - yMin + 1,
                horizontalDpi > 0 ? horizontalDpi : null, bitsPerPixel * planes,
                encoding == 1 ? "RLE" : "нет", "OK", extras);
    }
}
