package by.bsu.imageinfo.parser;

import by.bsu.imageinfo.ImageInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JpegParser {

    private static final boolean BE = false;
    private static final int EOI_SEARCH = 1024;

    private JpegParser() {
    }

    public static ImageInfo parse(BinaryFile f, Path path) throws IOException {
        Integer width = null;
        Integer height = null;
        Integer depth = null;
        String compression = null;
        Integer dpi = null;
        Map<String, String> extras = new LinkedHashMap<>();
        TiffParser.Ifd exif = null;

        long position = 2;
        while (true) {
            if (f.u8(position) != 0xFF) {
                throw new IOException("Ожидался маркер JPEG на смещении " + position);
            }
            int marker = f.u8(position + 1);
            if (marker == 0xFF) {
                position++;
                continue;
            }
            if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
                position += 2;
                continue;
            }
            if (marker == 0xD9 || marker == 0xDA) {
                break;
            }
            int length = f.u16(position + 2, BE);
            long data = position + 4;

            if (isStartOfFrame(marker)) {
                int precision = f.u8(data);
                height = f.u16(data + 1, BE);
                width = f.u16(data + 3, BE);
                int components = f.u8(data + 5);
                depth = precision * components;
                compression = switch (marker) {
                    case 0xC0, 0xC1 -> "DCT базовый";
                    case 0xC2 -> "DCT прогрессивный";
                    case 0xC3 -> "без потерь";
                    default -> String.format("SOF%d", marker - 0xC0);
                };
                Values.put(extras, "Цветовые компоненты", components + switch (components) {
                    case 1 -> " (Y — оттенки серого)";
                    case 3 -> " (Y, Cb, Cr)";
                    case 4 -> " (C, M, Y, K)";
                    default -> "";
                });
                if (components == 3) {
                    Values.put(extras, "Субдискретизация цветности", subsampling(f.u8(data + 7), f.u8(data + 10)));
                }
            } else if (marker == 0xE0 && f.matches(data, "JFIF\0")) {
                int units = f.u8(data + 7);
                int density = f.u16(data + 8, BE);
                if (density > 0 && units == 1) {
                    dpi = density;
                } else if (density > 0 && units == 2) {
                    dpi = Values.round(density * Values.INCH_IN_CM);
                }
            } else if (marker == 0xE1 && f.matches(data, "Exif\0\0")) {
                try {
                    exif = TiffParser.readFirstIfd(f, data + 6);
                } catch (IOException e) {
                    exif = null;
                }
            }
            position += 2 + length;
        }

        if (width == null) {
            throw new IOException("Нет маркера начала кадра SOF");
        }
        if (!hasEndMarker(f)) {
            throw new IOException("В конце JPEG нет маркера EOI (FF D9)");
        }
        if (exif != null) {
            if (dpi == null) {
                dpi = TiffParser.dpi(exif);
            }
            Values.put(extras, "Камера", Values.join(exif.text(TiffParser.MAKE), exif.text(TiffParser.MODEL)));
            Values.put(extras, "Дата", exif.text(TiffParser.DATE_TIME));
        }
        return new ImageInfo(path, "JPEG", width, height, dpi, depth, compression, "OK", extras);
    }

    private static boolean isStartOfFrame(int marker) {
        return marker >= 0xC0 && marker <= 0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC;
    }

    private static String subsampling(int ySampling, int cbSampling) {
        int yh = ySampling >> 4;
        int yv = ySampling & 0x0F;
        int ch = cbSampling >> 4;
        int cv = cbSampling & 0x0F;
        if (ch == 0 || cv == 0) {
            return null;
        }
        int h = yh / ch;
        int v = yv / cv;
        if (h == 1 && v == 1) {
            return "4:4:4";
        }
        if (h == 2 && v == 1) {
            return "4:2:2";
        }
        if (h == 2 && v == 2) {
            return "4:2:0";
        }
        return h + "×" + v;
    }

    private static boolean hasEndMarker(BinaryFile f) throws IOException {
        int length = (int) Math.min(EOI_SEARCH, f.size());
        byte[] tail = f.bytes(f.size() - length, length);
        for (int i = tail.length - 2; i >= 0; i--) {
            if ((tail[i] & 0xFF) == 0xFF && (tail[i + 1] & 0xFF) == 0xD9) {
                return true;
            }
        }
        return false;
    }
}
