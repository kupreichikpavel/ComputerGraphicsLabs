package by.bsu.imageinfo.parser;

import by.bsu.imageinfo.ImageInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TiffParser {

    static final int WIDTH = 256;
    static final int HEIGHT = 257;
    static final int BITS_PER_SAMPLE = 258;
    static final int COMPRESSION = 259;
    static final int PHOTOMETRIC = 262;
    static final int MAKE = 271;
    static final int MODEL = 272;
    static final int STRIP_OFFSETS = 273;
    static final int SAMPLES_PER_PIXEL = 277;
    static final int STRIP_BYTE_COUNTS = 279;
    static final int TILE_OFFSETS = 324;
    static final int TILE_BYTE_COUNTS = 325;
    static final int X_RESOLUTION = 282;
    static final int RESOLUTION_UNIT = 296;
    static final int DATE_TIME = 306;

    private static final int MAX_ENTRIES = 4096;
    private static final int MAX_STRIPS = 100_000;

    private TiffParser() {
    }

    record Entry(int type, long count, long valuePosition) {
    }

    static final class Ifd {

        private final BinaryFile f;
        private final boolean le;
        private final Map<Integer, Entry> entries;

        private Ifd(BinaryFile f, boolean le, Map<Integer, Entry> entries) {
            this.f = f;
            this.le = le;
            this.entries = entries;
        }

        Long number(int tag) throws IOException {
            Entry e = entries.get(tag);
            return e == null || e.count() == 0 ? null : number(e, 0);
        }

        long number(Entry e, int index) throws IOException {
            long position = e.valuePosition() + (long) index * typeSize(e.type());
            return switch (e.type()) {
                case 1, 6, 7 -> f.u8(position);
                case 3, 8 -> f.u16(position, le);
                case 4, 9 -> f.u32(position, le);
                default -> 0;
            };
        }

        Double rational(int tag) throws IOException {
            Entry e = entries.get(tag);
            if (e == null || e.type() != 5) {
                return null;
            }
            long numerator = f.u32(e.valuePosition(), le);
            long denominator = f.u32(e.valuePosition() + 4, le);
            return denominator == 0 ? null : (double) numerator / denominator;
        }

        String text(int tag) throws IOException {
            Entry e = entries.get(tag);
            return e == null || e.type() != 2 ? null : f.text(e.valuePosition(), (int) Math.min(e.count(), 200));
        }

        Entry entry(int tag) {
            return entries.get(tag);
        }
    }

    static Ifd readFirstIfd(BinaryFile f, long base) throws IOException {
        boolean le;
        if (f.matches(base, "II")) {
            le = true;
        } else if (f.matches(base, "MM")) {
            le = false;
        } else {
            throw new IOException("Неверный порядок байтов TIFF");
        }
        if (f.u16(base + 2, le) != 42) {
            throw new IOException("Неверное магическое число TIFF");
        }
        long ifd = base + f.u32(base + 4, le);
        int count = f.u16(ifd, le);
        if (count == 0 || count > MAX_ENTRIES) {
            throw new IOException("Некорректный каталог IFD");
        }
        Map<Integer, Entry> entries = new HashMap<>();
        for (int i = 0; i < count; i++) {
            long entry = ifd + 2 + i * 12L;
            int tag = f.u16(entry, le);
            int type = f.u16(entry + 2, le);
            long valueCount = f.u32(entry + 4, le);
            int size = typeSize(type);
            if (size == 0) {
                continue;
            }
            long valuePosition = valueCount * size <= 4 ? entry + 8 : base + f.u32(entry + 8, le);
            entries.put(tag, new Entry(type, valueCount, valuePosition));
        }
        return new Ifd(f, le, entries);
    }

    public static ImageInfo parse(BinaryFile f, Path path) throws IOException {
        Ifd ifd = readFirstIfd(f, 0);
        Long width = ifd.number(WIDTH);
        Long height = ifd.number(HEIGHT);
        if (width == null || height == null) {
            throw new IOException("Нет тегов ширины и высоты");
        }

        checkDataInsideFile(f, ifd);

        Long samplesTag = ifd.number(SAMPLES_PER_PIXEL);
        int samples = samplesTag == null ? 1 : samplesTag.intValue();
        Entry bits = ifd.entry(BITS_PER_SAMPLE);
        int depth = 0;
        if (bits == null) {
            depth = samples;
        } else {
            for (int i = 0; i < Math.min(bits.count(), 16); i++) {
                depth += (int) ifd.number(bits, i);
            }
            if (bits.count() == 1) {
                depth *= samples;
            }
        }

        Long code = ifd.number(COMPRESSION);
        int compression = code == null ? 1 : code.intValue();
        String compressionName = switch (compression) {
            case 1 -> "нет";
            case 2, 3, 4 -> "CCITT";
            case 5 -> "LZW";
            case 6, 7 -> "JPEG";
            case 8, 32946 -> "Deflate";
            case 32773 -> "PackBits";
            default -> "код " + compression;
        };

        Map<String, String> extras = new LinkedHashMap<>();
        Values.put(extras, "Каналов", String.valueOf(samples));
        Long photometric = ifd.number(PHOTOMETRIC);
        if (photometric != null) {
            Values.put(extras, "Цветовая модель", switch (photometric.intValue()) {
                case 0, 1 -> "оттенки серого";
                case 2 -> "RGB";
                case 3 -> "палитра";
                case 5 -> "CMYK";
                case 6 -> "YCbCr";
                default -> "код " + photometric;
            });
        }
        return new ImageInfo(path, "TIFF", width.intValue(), height.intValue(), dpi(ifd), depth,
                compressionName, "OK", extras);
    }

    private static void checkDataInsideFile(BinaryFile f, Ifd ifd) throws IOException {
        Entry offsets = ifd.entry(STRIP_OFFSETS) != null ? ifd.entry(STRIP_OFFSETS) : ifd.entry(TILE_OFFSETS);
        Entry counts = ifd.entry(STRIP_BYTE_COUNTS) != null ? ifd.entry(STRIP_BYTE_COUNTS) : ifd.entry(TILE_BYTE_COUNTS);
        if (offsets == null || counts == null) {
            throw new IOException("Нет тегов с расположением пиксельных данных");
        }
        long n = Math.min(Math.min(offsets.count(), counts.count()), MAX_STRIPS);
        for (int i = 0; i < n; i++) {
            if (ifd.number(offsets, i) + ifd.number(counts, i) > f.size()) {
                throw new IOException("Файл TIFF обрезан");
            }
        }
    }

    static Integer dpi(Ifd ifd) throws IOException {
        Double x = ifd.rational(X_RESOLUTION);
        if (x == null || x <= 0) {
            return null;
        }
        Long unit = ifd.number(RESOLUTION_UNIT);
        int u = unit == null ? 2 : unit.intValue();
        if (u == 2) {
            return Values.round(x);
        }
        if (u == 3) {
            return Values.round(x * Values.INCH_IN_CM);
        }
        return null;
    }

    static int typeSize(int type) {
        return switch (type) {
            case 1, 2, 6, 7 -> 1;
            case 3, 8 -> 2;
            case 4, 9, 11 -> 4;
            case 5, 10, 12 -> 8;
            default -> 0;
        };
    }
}
