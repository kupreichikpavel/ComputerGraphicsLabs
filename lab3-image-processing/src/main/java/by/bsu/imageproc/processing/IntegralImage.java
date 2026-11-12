package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;

public final class IntegralImage {

    private final int radius;
    private final int stride;
    private final long[] sum;
    private final long[] sumSquares;

    private IntegralImage(int radius, int stride, long[] sum, long[] sumSquares) {
        this.radius = radius;
        this.stride = stride;
        this.sum = sum;
        this.sumSquares = sumSquares;
    }

    public static IntegralImage build(GrayImage image, int radius, EdgeMode mode) {
        int width = image.width() + 2 * radius;
        int height = image.height() + 2 * radius;
        int stride = width + 1;
        long[] sum = new long[(height + 1) * stride];
        long[] sumSquares = new long[(height + 1) * stride];
        for (int y = 0; y < height; y++) {
            long rowSum = 0;
            long rowSquares = 0;
            int above = y * stride;
            int current = (y + 1) * stride;
            for (int x = 0; x < width; x++) {
                long v = image.get(x - radius, y - radius, mode);
                rowSum += v;
                rowSquares += v * v;
                sum[current + x + 1] = sum[above + x + 1] + rowSum;
                sumSquares[current + x + 1] = sumSquares[above + x + 1] + rowSquares;
            }
        }
        return new IntegralImage(radius, stride, sum, sumSquares);
    }

    public long windowSum(int x, int y) {
        return rect(sum, x, y);
    }

    public long windowSumSquares(int x, int y) {
        return rect(sumSquares, x, y);
    }

    public int windowArea() {
        int size = 2 * radius + 1;
        return size * size;
    }

    private long rect(long[] table, int x, int y) {
        int size = 2 * radius + 1;
        int x1 = x + size;
        int y1 = y + size;
        return table[y1 * stride + x1] - table[y * stride + x1] - table[y1 * stride + x] + table[y * stride + x];
    }
}
