package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThresholdTest {

    private static long[] histogram(int[][] classes) {
        long[] h = new long[256];
        Random random = new Random(1);
        for (int[] c : classes) {
            for (int i = 0; i < c[2]; i++) {
                int v = (int) Math.round(c[0] + random.nextGaussian() * c[1]);
                h[Math.max(0, Math.min(255, v))]++;
            }
        }
        return h;
    }

    @Test
    void bothMethodsSplitTwoPeaks() {
        long[] h = histogram(new int[][]{{60, 10, 5000}, {190, 10, 5000}});
        int otsu = GlobalThreshold.otsu(h).threshold();
        int iterative = GlobalThreshold.iterative(h).threshold();
        assertTrue(otsu > 90 && otsu < 160, "Оцу: " + otsu);
        assertTrue(iterative > 90 && iterative < 160, "итеративный: " + iterative);
    }

    @Test
    void otsuSeparabilityIsHighForCleanPeaks() {
        long[] h = histogram(new int[][]{{40, 5, 5000}, {210, 5, 5000}});
        assertTrue(GlobalThreshold.otsu(h).separability() > 0.95);
    }

    @Test
    void methodsDifferOnThreePeaks() {
        long[] h = histogram(new int[][]{{45, 5, 900}, {190, 5, 4550}, {245, 5, 4550}});
        int otsu = GlobalThreshold.otsu(h).threshold();
        int iterative = GlobalThreshold.iterative(h).threshold();
        assertTrue(otsu < 150, "Оцу должен отделить тёмный класс: " + otsu);
        assertTrue(iterative > 190 && iterative < 245, "итеративный застревает между светлыми: " + iterative);
    }

    @Test
    void binarizeUsesStrictlyGreater() {
        GrayImage image = new GrayImage(3, 1, new int[]{99, 100, 101});
        assertEquals(0, GlobalThreshold.binarize(image, 100, Progress.NONE).get(1, 0));
        assertEquals(255, GlobalThreshold.binarize(image, 100, Progress.NONE).get(2, 0));
    }

    @Test
    void integralImageMatchesBruteForce() {
        Random random = new Random(3);
        int w = 23;
        int h = 17;
        int r = 4;
        int[] data = new int[w * h];
        for (int i = 0; i < data.length; i++) {
            data[i] = random.nextInt(256);
        }
        GrayImage image = new GrayImage(w, h, data);
        for (EdgeMode mode : EdgeMode.values()) {
            IntegralImage sat = IntegralImage.build(image, r, mode);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    long sum = 0;
                    long squares = 0;
                    for (int dy = -r; dy <= r; dy++) {
                        for (int dx = -r; dx <= r; dx++) {
                            long v = image.get(x + dx, y + dy, mode);
                            sum += v;
                            squares += v * v;
                        }
                    }
                    assertEquals(sum, sat.windowSum(x, y));
                    assertEquals(squares, sat.windowSumSquares(x, y));
                }
            }
        }
    }

    @Test
    void adaptiveThresholdHandlesIlluminationGradient() {
        int w = 200;
        int h = 60;
        int[] data = new int[w * h];
        boolean[] text = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int light = 60 + x;
                boolean ink = (x / 6) % 3 == 0 && y > 20 && y < 40;
                text[y * w + x] = ink;
                data[y * w + x] = ink ? light / 2 : light;
            }
        }
        GrayImage image = new GrayImage(w, h, data);
        GrayImage adaptive = AdaptiveThreshold.sauvola(image, 10, 0.2, EdgeMode.REFLECT, Progress.NONE).gray();
        GrayImage global = GlobalThreshold.applyOtsu(image, Progress.NONE).gray();
        int adaptiveErrors = 0;
        int globalErrors = 0;
        for (int i = 0; i < data.length; i++) {
            boolean background = !text[i];
            if ((adaptive.data()[i] == 255) != background) {
                adaptiveErrors++;
            }
            if ((global.data()[i] == 255) != background) {
                globalErrors++;
            }
        }
        assertTrue(adaptiveErrors * 10 < globalErrors, "адаптивный " + adaptiveErrors + ", глобальный " + globalErrors);
    }
}
