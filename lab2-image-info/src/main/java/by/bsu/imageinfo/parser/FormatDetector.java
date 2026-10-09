package by.bsu.imageinfo.parser;

import java.io.IOException;

public final class FormatDetector {

    private FormatDetector() {
    }

    public static String detect(BinaryFile file) throws IOException {
        long size = file.size();
        if (size >= 2 && file.matches(0, "BM")) {
            return "BMP";
        }
        if (size >= 8 && file.u8(0) == 0x89 && file.matches(1, "PNG\r\n\u001A\n")) {
            return "PNG";
        }
        if (size >= 3 && file.u8(0) == 0xFF && file.u8(1) == 0xD8 && file.u8(2) == 0xFF) {
            return "JPEG";
        }
        if (size >= 6 && (file.matches(0, "GIF87a") || file.matches(0, "GIF89a"))) {
            return "GIF";
        }
        if (size >= 4 && (file.matches(0, "II*\0") || file.matches(0, "MM\0*"))) {
            return "TIFF";
        }
        if (size >= 128 && file.u8(0) == 0x0A && isPcxHeader(file)) {
            return "PCX";
        }
        return null;
    }

    private static boolean isPcxHeader(BinaryFile file) throws IOException {
        int version = file.u8(1);
        int encoding = file.u8(2);
        int bits = file.u8(3);
        return (version == 0 || version == 2 || version == 3 || version == 4 || version == 5)
                && (encoding == 0 || encoding == 1)
                && (bits == 1 || bits == 2 || bits == 4 || bits == 8);
    }
}
