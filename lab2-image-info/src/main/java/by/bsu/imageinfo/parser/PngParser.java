package by.bsu.imageinfo.parser;

import by.bsu.imageinfo.ImageInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PngParser {

    private static final boolean BE = false;
    private static final int MAX_CHUNKS = 1_000_000;

    private PngParser() {
    }

    public static ImageInfo parse(BinaryFile f, Path path) throws IOException {
        if (f.u32(8, BE) != 13 || !f.matches(12, "IHDR")) {
            throw new IOException("Первым чанком PNG должен быть IHDR");
        }
        long width = f.u32(16, BE);
        long height = f.u32(20, BE);
        int bitsPerChannel = f.u8(24);
        int colorType = f.u8(25);
        int interlace = f.u8(28);

        int channels = switch (colorType) {
            case 0, 3 -> 1;
            case 2 -> 3;
            case 4 -> 2;
            case 6 -> 4;
            default -> throw new IOException("Недопустимый тип цвета PNG: " + colorType);
        };

        Map<String, String> extras = new LinkedHashMap<>();
        Values.put(extras, "Тип цвета", colorType + switch (colorType) {
            case 0 -> " — оттенки серого";
            case 2 -> " — RGB";
            case 3 -> " — палитра";
            case 4 -> " — оттенки серого + альфа";
            default -> " — RGBA";
        });
        Values.put(extras, "Чересстрочность", interlace == 1 ? "Adam7" : "нет");

        Integer dpi = null;
        boolean end = false;
        long position = 8;
        for (int i = 0; i < MAX_CHUNKS && position + 8 <= f.size(); i++) {
            long length = f.u32(position, BE);
            long data = position + 8;
            if (data + length + 4 > f.size()) {
                throw new IOException("Файл PNG обрезан");
            }
            if (f.matches(position + 4, "PLTE")) {
                Values.put(extras, "Размер палитры", Values.colors(length / 3));
            } else if (f.matches(position + 4, "pHYs") && length >= 9 && f.u8(data + 8) == 1) {
                dpi = Values.round(f.u32(data, BE) * Values.INCH_IN_METERS);
            } else if (f.matches(position + 4, "IEND")) {
                end = true;
                break;
            }
            position = data + length + 4;
        }
        if (!end) {
            throw new IOException("В конце PNG нет чанка IEND");
        }
        return new ImageInfo(path, "PNG", (int) width, (int) height, dpi, bitsPerChannel * channels,
                "Deflate", "OK", extras);
    }
}
