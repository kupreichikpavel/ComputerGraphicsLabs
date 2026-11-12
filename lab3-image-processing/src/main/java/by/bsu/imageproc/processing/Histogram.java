package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.GrayImage;

public final class Histogram {

    public static final int LEVELS = 256;

    private Histogram() {
    }

    public static long[] of(GrayImage image) {
        long[] histogram = new long[LEVELS];
        for (int value : image.data()) {
            histogram[Math.max(0, Math.min(255, value))]++;
        }
        return histogram;
    }
}
