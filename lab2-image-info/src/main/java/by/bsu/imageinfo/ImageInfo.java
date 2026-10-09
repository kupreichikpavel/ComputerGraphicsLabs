package by.bsu.imageinfo;

import java.nio.file.Path;
import java.util.Map;

public record ImageInfo(
    Path path,
    String format,
    Integer width,
    Integer height,
    Integer dpi,
    Integer bitDepth,
    String compression,
    String status,
    Map<String, String> extras
) {

    public static ImageInfo failed(Path path, String status) {
        return new ImageInfo(path, null, null, null, null, null, null, status, Map.of());
    }

    public String name() {
        return path.getFileName().toString();
    }

    public String size() {
        return width == null || height == null ? null : width + " × " + height;
    }

    public long pixels() {
        return width == null || height == null ? -1 : (long) width * height;
    }
}