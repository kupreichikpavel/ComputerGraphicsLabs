package by.bsu.imageinfo;

import by.bsu.imageinfo.parser.BinaryFile;
import by.bsu.imageinfo.parser.BmpParser;
import by.bsu.imageinfo.parser.FormatDetector;
import by.bsu.imageinfo.parser.GifParser;
import by.bsu.imageinfo.parser.JpegParser;
import by.bsu.imageinfo.parser.PcxParser;
import by.bsu.imageinfo.parser.PngParser;
import by.bsu.imageinfo.parser.TiffParser;

import java.nio.file.Path;

public final class ImageInfoReader {

    public ImageInfo read(Path path) {
        try (BinaryFile file = BinaryFile.open(path)) {
            String format = FormatDetector.detect(file);
            if (format == null) {
                return ImageInfo.failed(path, "Не изображение");
            }
            return switch (format) {
                case "BMP" -> BmpParser.parse(file, path);
                case "PNG" -> PngParser.parse(file, path);
                case "JPEG" -> JpegParser.parse(file, path);
                case "GIF" -> GifParser.parse(file, path);
                case "TIFF" -> TiffParser.parse(file, path);
                default -> PcxParser.parse(file, path);
            };
        } catch (Exception e) {
            return ImageInfo.failed(path, "Повреждён");
        }
    }
}
