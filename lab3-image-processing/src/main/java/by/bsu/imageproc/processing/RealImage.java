package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;

public final class RealImage {

    private final int width;
    private final int height;
    private final double[] data;

    public RealImage(int width, int height) {
        this.width = width;
        this.height = height;
        this.data = new double[width * height];
    }

    public static RealImage of(GrayImage image) {
        RealImage result = new RealImage(image.width(), image.height());
        int[] src = image.data();
        for (int i = 0; i < src.length; i++) {
            result.data[i] = src[i];
        }
        return result;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public double[] data() {
        return data;
    }

    public double get(int x, int y) {
        return data[y * width + x];
    }

    public double get(int x, int y, EdgeMode mode) {
        return data[mode.index(y, height) * width + mode.index(x, width)];
    }

    public double max() {
        double max = 0;
        for (double v : data) {
            max = Math.max(max, v);
        }
        return max;
    }

    public GrayImage normalized() {
        double max = max();
        int[] out = new int[data.length];
        if (max > 0) {
            for (int i = 0; i < data.length; i++) {
                out[i] = (int) Math.round(Math.max(0, data[i]) * 255 / max);
            }
        }
        return new GrayImage(width, height, out);
    }

    public GrayImage thresholded(double threshold) {
        int[] out = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = data[i] >= threshold && data[i] > 0 ? 255 : 0;
        }
        return new GrayImage(width, height, out);
    }
}
