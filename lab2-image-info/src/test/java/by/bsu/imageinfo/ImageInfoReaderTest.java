package by.bsu.imageinfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageInfoReaderTest {

    @TempDir
    Path dir;

    private final ImageInfoReader reader = new ImageInfoReader();

    private ImageInfo write(String format, int type) throws IOException {
        Path file = dir.resolve("image." + format);
        ImageIO.write(new BufferedImage(40, 30, type), format, file.toFile());
        return reader.read(file);
    }

    @Test
    void readsPng() throws IOException {
        ImageInfo info = write("png", BufferedImage.TYPE_INT_ARGB);
        assertEquals("PNG", info.format());
        assertEquals("40 × 30", info.size());
        assertEquals(32, info.bitDepth());
        assertEquals("Deflate", info.compression());
    }

    @Test
    void readsBmp() throws IOException {
        ImageInfo info = write("bmp", BufferedImage.TYPE_3BYTE_BGR);
        assertEquals("BMP", info.format());
        assertEquals("40 × 30", info.size());
        assertEquals(24, info.bitDepth());
    }

    @Test
    void readsJpeg() throws IOException {
        ImageInfo info = write("jpg", BufferedImage.TYPE_INT_RGB);
        assertEquals("JPEG", info.format());
        assertEquals("40 × 30", info.size());
        assertEquals(24, info.bitDepth());
    }

    @Test
    void readsGif() throws IOException {
        ImageInfo info = write("gif", BufferedImage.TYPE_BYTE_INDEXED);
        assertEquals("GIF", info.format());
        assertEquals("40 × 30", info.size());
        assertEquals("LZW", info.compression());
    }

    @Test
    void textFileIsNotAnImage() throws IOException {
        Path file = dir.resolve("fake.jpg");
        Files.writeString(file, "это не картинка\n".repeat(20));
        assertEquals("Не изображение", reader.read(file).status());
    }
}