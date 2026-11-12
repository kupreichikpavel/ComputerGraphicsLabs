package by.bsu.imageproc.buffer;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

public final class FxImages {

    private FxImages() {
    }

    public static ColorImage read(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        PixelReader reader = image.getPixelReader();
        if (reader == null) {
            throw new IllegalArgumentException("Не удалось получить пиксели изображения");
        }
        int[] argb = new int[width * height];
        reader.getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), argb, 0, width);
        return new ColorImage(width, height, argb);
    }

    public static WritableImage write(ColorImage image) {
        WritableImage result = new WritableImage(image.width(), image.height());
        result.getPixelWriter().setPixels(0, 0, image.width(), image.height(),
                PixelFormat.getIntArgbInstance(), image.data(), 0, image.width());
        return result;
    }
}
