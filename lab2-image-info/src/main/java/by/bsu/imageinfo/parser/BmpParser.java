package by.bsu.imageinfo.parser;

import by.bsu.imageinfo.ImageInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BmpParser {

    private static final boolean LE = true;

    private BmpParser() {
    }

    public static ImageInfo parse(BinaryFile f, Path path) throws IOException {
        long fileSize = f.u32(2, LE);
        long headerSize = f.u32(14, LE);

        int width;
        int height;
        int bits;
        long compression = 0;
        int pixelsPerMeter = 0;
        long colorsUsed = 0;

        if (headerSize == 12) {
            width = f.u16(18, LE);
            height = f.u16(20, LE);
            bits = f.u16(24, LE);
        } else if (headerSize >= 40) {
            width = f.s32(18, LE);
            height = f.s32(22, LE);
            bits = f.u16(28, LE);
            compression = f.u32(30, LE);
            pixelsPerMeter = f.s32(38, LE);
            colorsUsed = f.u32(46, LE);
        } else {
            throw new IOException("Неизвестный размер заголовка BMP: " + headerSize);
        }
        if (width <= 0 || height == 0 || bits == 0) {
            throw new IOException("Некорректный заголовок BMP");
        }
        if (fileSize > f.size()) {
            throw new IOException("Файл BMP обрезан");
        }

        String compressionName = switch ((int) compression) {
            case 0 -> "нет (BI_RGB)";
            case 1 -> "RLE8";
            case 2 -> "RLE4";
            case 3 -> "нет (BI_BITFIELDS)";
            case 4 -> "JPEG";
            case 5 -> "PNG";
            default -> "код " + compression;
        };

        Map<String, String> extras = new LinkedHashMap<>();
        if (bits <= 8) {
            Values.put(extras, "Размер палитры", Values.colors(colorsUsed > 0 ? colorsUsed : 1L << bits));
        }
        Values.put(extras, "Порядок строк", height < 0 ? "сверху вниз" : "снизу вверх");

        Integer dpi = pixelsPerMeter > 0 ? Values.round(pixelsPerMeter * Values.INCH_IN_METERS) : null;
        return new ImageInfo(path, "BMP", width, Math.abs(height), dpi, bits, compressionName, "OK", extras);
    }
}
