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

    @Test
    void windowShapesHaveExpectedSizes() {
        assertEquals(9, WindowShape.SQUARE.of(1).size());
        assertEquals(25, WindowShape.SQUARE.of(2).size());
        assertEquals(9, WindowShape.DISK.of(1).size());
        assertEquals(21, WindowShape.DISK.of(2).size());
    }

    @Test
    void medianMinMaxOfHandMadeWindow() {
        GrayImage image = new GrayImage(3, 3, new int[]{
                12, 15, 14,
                255, 13, 16,
                0, 14, 15});
        WindowShape.Window window = WindowShape.SQUARE.of(1);
        assertEquals(14, OrderFilter.apply(image, window, OrderFilter.Rank.MEDIAN, EdgeMode.CLAMP, Progress.NONE).get(1, 1));
        assertEquals(0, OrderFilter.apply(image, window, OrderFilter.Rank.MIN, EdgeMode.CLAMP, Progress.NONE).get(1, 1));
        assertEquals(255, OrderFilter.apply(image, window, OrderFilter.Rank.MAX, EdgeMode.CLAMP, Progress.NONE).get(1, 1));
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
        GrayImage result = OrderFilter.apply(new GrayImage(w, h, data), WindowShape.SQUARE.of(1),
                OrderFilter.Rank.MEDIAN, EdgeMode.REFLECT, Progress.NONE);
        assertArrayEquals(clean, result.data());
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
                        if (dx * dx + dy * dy <= 6) {
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
        GrayImage opened = Morphology.opening(image, window, EdgeMode.REFLECT, Progress.NONE);
        assertArrayEquals(opened.data(), Morphology.opening(opened, window, EdgeMode.REFLECT, Progress.NONE).data());
        GrayImage closed = Morphology.closing(image, window, EdgeMode.REFLECT, Progress.NONE);
        assertArrayEquals(closed.data(), Morphology.closing(closed, window, EdgeMode.REFLECT, Progress.NONE).data());
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
    void gradientIsZeroInsideFlatRegions() {
        int w = 30;
        int h = 20;
        int[] data = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                data[y * w + x] = x < 15 ? 50 : 200;
            }
        }
        GrayImage g = Morphology.apply(new GrayImage(w, h, data), Morphology.Operation.GRADIENT,
                WindowShape.SQUARE.of(1), EdgeMode.REFLECT, Progress.NONE);
        assertEquals(0, g.get(5, 10));
        assertEquals(0, g.get(25, 10));
        assertEquals(150, g.get(14, 10));
        assertEquals(150, g.get(15, 10));
    }

    @Test
    void parallelResultEqualsSingleThread() {
        GrayImage image = random(300, 211, 13);
        int before = Parallel.threads();
        try {
            Parallel.setThreads(1);
            int[] single = OrderFilter.apply(image, WindowShape.DISK.of(3), OrderFilter.Rank.MEDIAN, EdgeMode.REFLECT,
                    Progress.NONE).data();
            Parallel.setThreads(6);
            int[] parallel = OrderFilter.apply(image, WindowShape.DISK.of(3), OrderFilter.Rank.MEDIAN, EdgeMode.REFLECT,
                    Progress.NONE).data();
            assertArrayEquals(single, parallel);
        } finally {
            Parallel.setThreads(before);
        }
    }
}
