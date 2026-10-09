package by.bsu.imageinfo;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.Rational;
import com.drew.metadata.Metadata;
import com.drew.metadata.bmp.BmpHeaderDirectory;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.gif.GifHeaderDirectory;
import com.drew.metadata.gif.GifImageDirectory;
import com.drew.metadata.jfif.JfifDirectory;
import com.drew.metadata.jpeg.JpegComponent;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.pcx.PcxDirectory;
import com.drew.metadata.png.PngDirectory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ImageInfoReader {

    private static final double INCH_IN_METERS = 0.0254;
    private static final double INCH_IN_CM = 2.54;
    private static final Set<FileType> TIFF_TYPES =
        Set.of(FileType.Tiff, FileType.Arw, FileType.Cr2, FileType.Nef, FileType.Orf, FileType.Rw2);

    public ImageInfo read(Path path) {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
            FileType type = FileTypeDetector.detectFileType(in);
            if (TIFF_TYPES.contains(type)) {
                type = FileType.Tiff;
            }
            if (!Set.of(FileType.Jpeg, FileType.Png, FileType.Gif, FileType.Bmp, FileType.Tiff, FileType.Pcx).contains(type)) {
                return ImageInfo.failed(path, "Не изображение");
            }
            Metadata metadata = ImageMetadataReader.readMetadata(in, Files.size(path), type);
            ImageInfo info = switch (type) {
                case Jpeg -> readJpeg(path, metadata);
                case Png -> readPng(path, metadata);
                case Gif -> readGif(path, metadata);
                case Bmp -> readBmp(path, metadata);
                case Tiff -> readTiff(path, metadata);
                default -> readPcx(path, metadata);
            };
            return info != null ? info : ImageInfo.failed(path, "Повреждён");
        } catch (Exception e) {
            return ImageInfo.failed(path, "Повреждён");
        }
    }

    private static ImageInfo readJpeg(Path path, Metadata metadata) {
        JpegDirectory jpeg = metadata.getFirstDirectoryOfType(JpegDirectory.class);
        if (jpeg == null) {
            return null;
        }
        Integer precision = jpeg.getInteger(JpegDirectory.TAG_DATA_PRECISION);
        Integer components = jpeg.getInteger(JpegDirectory.TAG_NUMBER_OF_COMPONENTS);
        Integer depth = precision == null || components == null ? null : precision * components;
        Integer sof = jpeg.getInteger(JpegDirectory.TAG_COMPRESSION_TYPE);
        String compression = sof == null ? null : switch (sof) {
            case 0, 1 -> "DCT базовый";
            case 2 -> "DCT прогрессивный";
            case 3 -> "без потерь";
            default -> "SOF" + sof;
        };

        Integer dpi = null;
        JfifDirectory jfif = metadata.getFirstDirectoryOfType(JfifDirectory.class);
        if (jfif != null) {
            Integer units = jfif.getInteger(JfifDirectory.TAG_UNITS);
            Integer x = jfif.getInteger(JfifDirectory.TAG_RESX);
            if (units != null && x != null && x > 0) {
                if (units == 1) {
                    dpi = x;
                } else if (units == 2) {
                    dpi = round(x * INCH_IN_CM);
                }
            }
        }
        ExifIFD0Directory exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (dpi == null) {
            dpi = exifDpi(exif);
        }

        Map<String, String> extras = new LinkedHashMap<>();
        if (components != null) {
            put(extras, "Цветовые компоненты", components + switch (components) {
                case 1 -> " (Y — оттенки серого)";
                case 3 -> " (Y, Cb, Cr)";
                case 4 -> " (C, M, Y, K)";
                default -> "";
            });
        }
        if (components != null && components == 3) {
            put(extras, "Субдискретизация цветности", subsampling(jpeg.getComponent(0), jpeg.getComponent(1)));
        }
        if (exif != null) {
            put(extras, "Камера", join(exif.getString(ExifIFD0Directory.TAG_MAKE), exif.getString(ExifIFD0Directory.TAG_MODEL)));
            put(extras, "Дата", exif.getString(ExifIFD0Directory.TAG_DATETIME));
        }
        return new ImageInfo(path, "JPEG",
            jpeg.getInteger(JpegDirectory.TAG_IMAGE_WIDTH), jpeg.getInteger(JpegDirectory.TAG_IMAGE_HEIGHT),
            dpi, depth, compression, "OK", extras);
    }

    private static ImageInfo readPng(Path path, Metadata metadata) {
        Integer width = null;
        Integer height = null;
        Integer depth = null;
        Integer dpi = null;
        Map<String, String> extras = new LinkedHashMap<>();
        for (PngDirectory png : metadata.getDirectoriesOfType(PngDirectory.class)) {
            if (png.containsTag(PngDirectory.TAG_IMAGE_WIDTH)) {
                width = png.getInteger(PngDirectory.TAG_IMAGE_WIDTH);
                height = png.getInteger(PngDirectory.TAG_IMAGE_HEIGHT);
                Integer bits = png.getInteger(PngDirectory.TAG_BITS_PER_SAMPLE);
                Integer colorType = png.getInteger(PngDirectory.TAG_COLOR_TYPE);
                if (bits != null && colorType != null) {
                    int channels = switch (colorType) {
                        case 2 -> 3;
                        case 4 -> 2;
                        case 6 -> 4;
                        default -> 1;
                    };
                    depth = bits * channels;
                    put(extras, "Тип цвета", colorType + switch (colorType) {
                        case 0 -> " — оттенки серого";
                        case 2 -> " — RGB";
                        case 3 -> " — палитра";
                        case 4 -> " — оттенки серого + альфа";
                        case 6 -> " — RGBA";
                        default -> "";
                    });
                }
                Integer interlace = png.getInteger(PngDirectory.TAG_INTERLACE_METHOD);
                put(extras, "Чересстрочность", interlace != null && interlace == 1 ? "Adam7" : "нет");
            }
            Integer palette = png.getInteger(PngDirectory.TAG_PALETTE_SIZE);
            if (palette != null) {
                put(extras, "Размер палитры", colors(palette));
            }
            Integer unit = png.getInteger(PngDirectory.TAG_UNIT_SPECIFIER);
            Integer x = png.getInteger(PngDirectory.TAG_PIXELS_PER_UNIT_X);
            if (unit != null && unit == 1 && x != null) {
                dpi = round(x * INCH_IN_METERS);
            }
        }
        if (width == null) {
            return null;
        }
        return new ImageInfo(path, "PNG", width, height, dpi, depth, "Deflate", "OK", extras);
    }

    private static ImageInfo readGif(Path path, Metadata metadata) {
        GifHeaderDirectory header = metadata.getFirstDirectoryOfType(GifHeaderDirectory.class);
        if (header == null) {
            return null;
        }
        Map<String, String> extras = new LinkedHashMap<>();
        put(extras, "Версия GIF", header.getString(GifHeaderDirectory.TAG_GIF_FORMAT_VERSION));
        Integer depth = null;
        Integer paletteSize = header.getInteger(GifHeaderDirectory.TAG_COLOR_TABLE_SIZE);
        if (Boolean.TRUE.equals(header.getBooleanObject(GifHeaderDirectory.TAG_HAS_GLOBAL_COLOR_TABLE)) && paletteSize != null) {
            depth = Integer.numberOfTrailingZeros(paletteSize);
            put(extras, "Размер палитры", colors(paletteSize));
        } else {
            GifImageDirectory frame = metadata.getFirstDirectoryOfType(GifImageDirectory.class);
            if (frame != null) {
                depth = frame.getInteger(GifImageDirectory.TAG_LOCAL_COLOUR_TABLE_BITS_PER_PIXEL);
            }
        }
        return new ImageInfo(path, "GIF",
            header.getInteger(GifHeaderDirectory.TAG_IMAGE_WIDTH), header.getInteger(GifHeaderDirectory.TAG_IMAGE_HEIGHT),
            null, depth, "LZW", "OK", withFrames(extras, metadata.getDirectoriesOfType(GifImageDirectory.class).size()));
    }

    private static ImageInfo readBmp(Path path, Metadata metadata) {
        BmpHeaderDirectory bmp = metadata.getFirstDirectoryOfType(BmpHeaderDirectory.class);
        if (bmp == null) {
            return null;
        }
        Integer height = bmp.getInteger(BmpHeaderDirectory.TAG_IMAGE_HEIGHT);
        Integer x = bmp.getInteger(BmpHeaderDirectory.TAG_X_PIXELS_PER_METER);
        Integer code = bmp.getInteger(BmpHeaderDirectory.TAG_COMPRESSION);
        String compression = code == null ? null : switch (code) {
            case 0 -> "нет (BI_RGB)";
            case 1 -> "RLE8";
            case 2 -> "RLE4";
            case 3 -> "нет (BI_BITFIELDS)";
            case 4 -> "JPEG";
            case 5 -> "PNG";
            default -> "код " + code;
        };
        Integer bits = bmp.getInteger(BmpHeaderDirectory.TAG_BITS_PER_PIXEL);
        Map<String, String> extras = new LinkedHashMap<>();
        if (bits != null && bits <= 8) {
            Integer used = bmp.getInteger(BmpHeaderDirectory.TAG_PALETTE_COLOUR_COUNT);
            put(extras, "Размер палитры", colors(used != null && used > 0 ? used : 1 << bits));
        }
        if (height != null) {
            put(extras, "Порядок строк", height < 0 ? "сверху вниз" : "снизу вверх");
        }
        return new ImageInfo(path, "BMP",
            bmp.getInteger(BmpHeaderDirectory.TAG_IMAGE_WIDTH), height == null ? null : Math.abs(height),
            x != null && x > 0 ? round(x * INCH_IN_METERS) : null,
            bits, compression, "OK", extras);
    }

    private static ImageInfo readTiff(Path path, Metadata metadata) {
        ExifIFD0Directory ifd = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (ifd == null) {
            return null;
        }
        int[] bits = ifd.getIntArray(ExifIFD0Directory.TAG_BITS_PER_SAMPLE);
        Integer samples = ifd.getInteger(ExifIFD0Directory.TAG_SAMPLES_PER_PIXEL);
        int depth = 0;
        for (int b : bits == null ? new int[]{1} : bits) {
            depth += b;
        }
        if ((bits == null || bits.length == 1) && samples != null) {
            depth *= samples;
        }
        Integer code = ifd.getInteger(ExifIFD0Directory.TAG_COMPRESSION);
        String compression = switch (code == null ? 1 : code) {
            case 1 -> "нет";
            case 2, 3, 4 -> "CCITT";
            case 5 -> "LZW";
            case 6, 7 -> "JPEG";
            case 8, 32946 -> "Deflate";
            case 32773 -> "PackBits";
            default -> "код " + code;
        };
        Map<String, String> extras = new LinkedHashMap<>();
        put(extras, "Каналов", samples == null ? null : String.valueOf(samples));
        Integer photometric = ifd.getInteger(ExifIFD0Directory.TAG_PHOTOMETRIC_INTERPRETATION);
        if (photometric != null) {
            put(extras, "Цветовая модель", switch (photometric) {
                case 0, 1 -> "оттенки серого";
                case 2 -> "RGB";
                case 3 -> "палитра";
                case 5 -> "CMYK";
                case 6 -> "YCbCr";
                default -> "код " + photometric;
            });
        }
        return new ImageInfo(path, "TIFF",
            ifd.getInteger(ExifIFD0Directory.TAG_IMAGE_WIDTH), ifd.getInteger(ExifIFD0Directory.TAG_IMAGE_HEIGHT),
            exifDpi(ifd), depth, compression, "OK", extras);
    }

    private static ImageInfo readPcx(Path path, Metadata metadata) {
        PcxDirectory pcx = metadata.getFirstDirectoryOfType(PcxDirectory.class);
        if (pcx == null) {
            return null;
        }
        Integer xMin = pcx.getInteger(PcxDirectory.TAG_XMIN);
        Integer yMin = pcx.getInteger(PcxDirectory.TAG_YMIN);
        Integer xMax = pcx.getInteger(PcxDirectory.TAG_XMAX);
        Integer yMax = pcx.getInteger(PcxDirectory.TAG_YMAX);
        Integer bits = pcx.getInteger(PcxDirectory.TAG_BITS_PER_PIXEL);
        Integer planes = pcx.getInteger(PcxDirectory.TAG_COLOR_PLANES);
        Integer dpi = pcx.getInteger(PcxDirectory.TAG_HORIZONTAL_DPI);
        boolean hasSize = xMin != null && yMin != null && xMax != null && yMax != null;
        return new ImageInfo(path, "PCX",
            hasSize ? xMax - xMin + 1 : null, hasSize ? yMax - yMin + 1 : null,
            dpi != null && dpi > 0 ? dpi : null,
            bits == null || planes == null ? null : bits * planes,
            "RLE", "OK", pcxExtras(pcx, planes));
    }

    private static Map<String, String> pcxExtras(PcxDirectory pcx, Integer planes) {
        Map<String, String> extras = new LinkedHashMap<>();
        put(extras, "Цветовых плоскостей", planes == null ? null : String.valueOf(planes));
        put(extras, "Версия PCX", pcx.getString(PcxDirectory.TAG_VERSION));
        return extras;
    }

    private static Map<String, String> withFrames(Map<String, String> extras, int frames) {
        put(extras, "Кадров", frames + (frames > 1 ? " (анимация)" : ""));
        return extras;
    }

    private static String subsampling(JpegComponent y, JpegComponent cb) {
        if (y == null || cb == null || cb.getHorizontalSamplingFactor() == 0 || cb.getVerticalSamplingFactor() == 0) {
            return null;
        }
        int h = y.getHorizontalSamplingFactor() / cb.getHorizontalSamplingFactor();
        int v = y.getVerticalSamplingFactor() / cb.getVerticalSamplingFactor();
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

    private static String colors(int count) {
        int mod100 = count % 100;
        int mod10 = count % 10;
        if (mod100 >= 11 && mod100 <= 14) {
            return count + " цветов";
        }
        if (mod10 == 1) {
            return count + " цвет";
        }
        if (mod10 >= 2 && mod10 <= 4) {
            return count + " цвета";
        }
        return count + " цветов";
    }

    private static String join(String a, String b) {
        String result = ((a == null ? "" : a.trim()) + " " + (b == null ? "" : b.trim())).trim();
        return result.isEmpty() ? null : result;
    }

    private static void put(Map<String, String> extras, String key, String value) {
        if (value != null && !value.isBlank()) {
            extras.put(key, value.trim());
        }
    }

    private static Integer exifDpi(ExifIFD0Directory ifd) {
        if (ifd == null) {
            return null;
        }
        Rational x = ifd.getRational(ExifIFD0Directory.TAG_X_RESOLUTION);
        if (x == null || x.doubleValue() <= 0) {
            return null;
        }
        Integer unit = ifd.getInteger(ExifIFD0Directory.TAG_RESOLUTION_UNIT);
        int u = unit == null ? 2 : unit;
        if (u == 2) {
            return round(x.doubleValue());
        }
        if (u == 3) {
            return round(x.doubleValue() * INCH_IN_CM);
        }
        return null;
    }

    private static int round(double value) {
        return (int) Math.round(value);
    }
}