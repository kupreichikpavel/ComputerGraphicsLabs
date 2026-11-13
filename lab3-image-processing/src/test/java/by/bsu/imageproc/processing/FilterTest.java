package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterTest {

    private static GrayImage random(int width, int height, long seed) {
        Random random = new Random(seed);
        int[] data = new int[width * height];
        for (int i = 0; i < data.length; i++) {
            data[i] = random.nextInt(256);
        }
        return new GrayImage(width, height, data);
    }

    private static GrayImage flat(int width, int height, int value) {
        int[] data = new int[width * height];
        java.util.Arrays.fill(data, value);
        return new GrayImage(width, height, data);
    }

    @Test
    void windowShapesHaveExpectedSizes() {
        assertEquals(25, WindowShape.SQUARE.of(2).size());
        assertEquals(9, WindowShape.CROSS.of(2).size());
        assertEquals(9, WindowShape.DISK.of(1).size());
        assertEquals(21, WindowShape.DISK.of(2).size());
    }

    @Test
    void medianOfHandMadeWindow() {
        GrayImage image = new GrayImage(3, 3, new int[]{
                12, 15, 14,
                255, 13, 16,
                0, 14, 15});
        GrayImage median = OrderFilter.huang(image, WindowShape.SQUARE.of(1), Statistic.MEDIAN, EdgeMode.CLAMP,
                Progress.NONE);
        assertEquals(14, median.get(1, 1));
        GrayImage min = OrderFilter.huang(image, WindowShape.SQUARE.of(1), Statistic.MIN, EdgeMode.CLAMP, Progress.NONE);
        GrayImage max = OrderFilter.huang(image, WindowShape.SQUARE.of(1), Statistic.MAX, EdgeMode.CLAMP, Progress.NONE);
        assertEquals(0, min.get(1, 1));
        assertEquals(255, max.get(1, 1));
    }

    @Test
    void huangEqualsNaiveSorting() {
        GrayImage image = random(37, 29, 7);
        Statistic[] statistics = {Statistic.MEDIAN, Statistic.MIN, Statistic.MAX, Statistic.MIDPOINT,
                Statistic.alphaTrimmed(0), Statistic.alphaTrimmed(0.2), Statistic.alphaTrimmed(0.45),
                Statistic.percentile(0.1), Statistic.percentile(0.9)};
        for (WindowShape shape : WindowShape.values()) {
            for (int r = 1; r <= 4; r++) {
                for (Statistic statistic : statistics) {
                    for (EdgeMode mode : EdgeMode.values()) {
                        WindowShape.Window window = shape.of(r);
                        assertArrayEquals(
                                OrderFilter.naive(image, window, statistic, mode, Progress.NONE).data(),
                                OrderFilter.huang(image, window, statistic, mode, Progress.NONE).data(),
                                shape + " r=" + r + " " + statistic + " " + mode);
                    }
                }
            }
        }
    }

    @Test
    void medianRemovesIsolatedImpulsesAndKeepsStep() {
        int w = 40;
        int h = 30;
        int[] data = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                data[y * w + x] = x < 20 ? 60 : 180;
            }
        }
        int[] clean = data.clone();
        for (int y = 2; y < h - 2; y += 4) {
            for (int x = 3; x < w - 3; x += 5) {
                data[y * w + x] = (x + y) % 2 == 0 ? 255 : 0;
            }
        }
        GrayImage result = OrderFilter.huang(new GrayImage(w, h, data), WindowShape.SQUARE.of(1), Statistic.MEDIAN,
                EdgeMode.REFLECT, Progress.NONE);
        assertArrayEquals(clean, result.data());
    }

    @Test
    void alphaTrimmedLimitsAreMeanAndMedian() {
        GrayImage image = random(21, 17, 3);
        WindowShape.Window window = WindowShape.SQUARE.of(1);
        GrayImage trimmedZero = OrderFilter.huang(image, window, Statistic.alphaTrimmed(0), EdgeMode.REFLECT, Progress.NONE);
        GrayImage trimmedMax = OrderFilter.huang(image, window, Statistic.alphaTrimmed(0.49), EdgeMode.REFLECT, Progress.NONE);
        GrayImage median = OrderFilter.huang(image, window, Statistic.MEDIAN, EdgeMode.REFLECT, Progress.NONE);
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                long sum = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        sum += image.get(x + dx, y + dy, EdgeMode.REFLECT);
                    }
                }
                assertEquals((sum + 4) / 9, trimmedZero.get(x, y));
            }
        }
        assertArrayEquals(median.data(), trimmedMax.data());
    }

    @Test
    void erosionAndDilationMatchDefinition() {
        GrayImage image = random(25, 19, 11);
        WindowShape.Window window = WindowShape.DISK.of(2);
        GrayImage eroded = Morphology.erode(image, window, EdgeMode.CLAMP, Progress.NONE);
        GrayImage dilated = Morphology.dilate(image, window, EdgeMode.CLAMP, Progress.NONE);
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                int min = 255;
                int max = 0;
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        if (window.contains(dx, dy)) {
                            int v = image.get(x + dx, y + dy, EdgeMode.CLAMP);
                            min = Math.min(min, v);
                            max = Math.max(max, v);
                        }
                    }
                }
                assertEquals(min, eroded.get(x, y));
                assertEquals(max, dilated.get(x, y));
            }
        }
    }

    @Test
    void openingAndClosingAreIdempotent() {
        GrayImage image = random(30, 24, 5);
        WindowShape.Window window = WindowShape.SQUARE.of(1);
        GrayImage opened = Morphology.apply(image, Morphology.Operation.OPENING, window, EdgeMode.REFLECT, Progress.NONE);
        GrayImage twice = Morphology.apply(opened, Morphology.Operation.OPENING, window, EdgeMode.REFLECT, Progress.NONE);
        assertArrayEquals(opened.data(), twice.data());
        GrayImage closed = Morphology.apply(image, Morphology.Operation.CLOSING, window, EdgeMode.REFLECT, Progress.NONE);
        GrayImage closedTwice = Morphology.apply(closed, Morphology.Operation.CLOSING, window, EdgeMode.REFLECT, Progress.NONE);
        assertArrayEquals(closed.data(), closedTwice.data());
    }

    @Test
    void topHatKeepsSmallDetailsAndRemovesRamp() {
        int w = 60;
        int h = 40;
        int[] data = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                data[y * w + x] = 40 + 2 * x;
            }
        }
        data[20 * w + 30] += 50;
        data[20 * w + 31] += 50;
        GrayImage hat = Morphology.apply(new GrayImage(w, h, data), Morphology.Operation.TOP_HAT,
                WindowShape.SQUARE.of(3), EdgeMode.REFLECT, Progress.NONE);
        assertTrue(hat.get(30, 20) >= 50);
        for (int y = 5; y < h - 5; y++) {
            for (int x = 5; x < w - 5; x++) {
                if (y != 20 || (x != 30 && x != 31)) {
                    assertEquals(0, hat.get(x, y), "x=" + x + " y=" + y);
                }
            }
        }
    }

    @Test
    void adaptiveMedianHandlesDenseNoise() {
        GrayImage clean = flat(80, 60, 120);
        GrayImage noisy = Noise.add(clean, Noise.Kind.SALT_PEPPER, 50, 42);
        GrayImage adaptive = AdaptiveMedian.filter(noisy, 5, EdgeMode.REFLECT, Progress.NONE, null);
        GrayImage median = OrderFilter.huang(noisy, WindowShape.SQUARE.of(1), Statistic.MEDIAN, EdgeMode.REFLECT,
                Progress.NONE);
        int restored = 0;
        int restoredByMedian = 0;
        for (int i = 0; i < clean.data().length; i++) {
            restored += adaptive.data()[i] == 120 ? 1 : 0;
            restoredByMedian += median.data()[i] == 120 ? 1 : 0;
        }
        assertTrue(restored > 0.99 * clean.data().length, "restored " + restored);
        assertTrue(restoredByMedian < restored);
    }

    @Test
    void separableConvolutionEqualsFull2d() {
        RealImage image = RealImage.of(random(31, 23, 9));
        double[] k = Convolution.gaussian1d(1.5);
        for (EdgeMode mode : EdgeMode.values()) {
            double[] separable = Convolution.separable(image, k, mode, Progress.NONE).data();
            double[] full = Convolution.apply(image, Kernel.outer(k), mode, Progress.NONE).data();
            for (int i = 0; i < full.length; i++) {
                assertEquals(full[i], separable[i], 1e-9);
            }
        }
    }

    @Test
    void parallelResultEqualsSingleThread() {
        GrayImage image = random(300, 211, 13);
        int before = Parallel.threads();
        try {
            Parallel.setThreads(1);
            int[] single = OrderFilter.huang(image, WindowShape.DISK.of(3), Statistic.MEDIAN, EdgeMode.REFLECT,
                    Progress.NONE).data();
            Parallel.setThreads(6);
            int[] parallel = OrderFilter.huang(image, WindowShape.DISK.of(3), Statistic.MEDIAN, EdgeMode.REFLECT,
                    Progress.NONE).data();
            assertArrayEquals(single, parallel);
        } finally {
            Parallel.setThreads(before);
        }
    }

    @Test
    void noiseIsReproducibleAndPsnrIsCorrect() {
        GrayImage clean = flat(100, 100, 128);
        GrayImage a = Noise.add(clean, Noise.Kind.SALT_PEPPER, 20, 1);
        GrayImage b = Noise.add(clean, Noise.Kind.SALT_PEPPER, 20, 1);
        assertArrayEquals(a.data(), b.data());
        int changed = 0;
        for (int v : a.data()) {
            changed += v != 128 ? 1 : 0;
        }
        assertEquals(2000, changed, 200);
        assertEquals(Double.POSITIVE_INFINITY, Quality.psnr(clean, clean));
        GrayImage shifted = flat(100, 100, 138);
        assertEquals(10 * Math.log10(255.0 * 255.0 / 100.0), Quality.psnr(clean, shifted), 1e-9);
    }
}
