package by.bsu.imageproc.buffer;

public final class GrayImage {

    private final int width;
    private final int height;
    private final int[] data;

    public GrayImage(int width, int height) {
        this(width, height, new int[width * height]);
    }

    public GrayImage(int width, int height, int[] data) {
        if (data.length != width * height) {
            throw new IllegalArgumentException("Размер массива не совпадает с размером изображения");
        }
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public static GrayImage fromArgb(ColorImage color) {
        int[] argb = color.data();
        int[] gray = new int[argb.length];
        for (int i = 0; i < argb.length; i++) {
            int p = argb[i];
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;
            gray[i] = (299 * r + 587 * g + 114 * b + 500) / 1000;
        }
        return new GrayImage(color.width(), color.height(), gray);
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

    public int get(int x, int y) {
        return data[y * width + x];
    }

    public int get(int x, int y, EdgeMode mode) {
        return data[mode.index(y, height) * width + mode.index(x, width)];
    }

    public void set(int x, int y, int value) {
        data[y * width + x] = value;
    }

    public ColorImage toColor() {
        int[] argb = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            int v = Math.max(0, Math.min(255, data[i]));
            argb[i] = 0xFF000000 | (v << 16) | (v << 8) | v;
        }
        return new ColorImage(width, height, argb);
    }
}
