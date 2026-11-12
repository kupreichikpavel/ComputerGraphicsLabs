package by.bsu.imageproc.buffer;

public final class ColorImage {

    private final int width;
    private final int height;
    private final int[] data;

    public ColorImage(int width, int height) {
        this(width, height, new int[width * height]);
    }

    public ColorImage(int width, int height, int[] data) {
        if (data.length != width * height) {
            throw new IllegalArgumentException("Размер массива не совпадает с размером изображения");
        }
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int[] data() {
        return data;
    }

    public void set(int x, int y, int argb) {
        data[y * width + x] = argb;
    }

    public static int rgb(int r, int g, int b) {
        return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(255, v);
    }
}
