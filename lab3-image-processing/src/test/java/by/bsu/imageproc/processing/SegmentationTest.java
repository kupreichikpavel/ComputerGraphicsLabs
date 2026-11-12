package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SegmentationTest {

    private static GrayImage flat(int w, int h, int value) {
        int[] data = new int[w * h];
        java.util.Arrays.fill(data, value);
        return new GrayImage(w, h, data);
    }

    private static int count(GrayImage image) {
        int n = 0;
        for (int v : image.data()) {
            if (v > 0) {
                n++;
            }
        }
        return n;
    }

    @Test
    void separableGaussianEqualsFull2dConvolution() {
        Random random = new Random(5);
        RealImage image = new RealImage(30, 20);
        for (int i = 0; i < image.data().length; i++) {
            image.data()[i] = random.nextInt(256);
        }
        double[] k1 = Convolution.gaussian1d(1.5);
        double[] k2 = new double[k1.length * k1.length];
        for (int y = 0; y < k1.length; y++) {
            for (int x = 0; x < k1.length; x++) {
                k2[y * k1.length + x] = k1[y] * k1[x];
            }
        }
        Kernel full = new Kernel(k1.length, k1.length, k1.length / 2, k1.length / 2, k2);
        for (EdgeMode mode : EdgeMode.values()) {
            RealImage a = Convolution.separable(image, k1, mode, Progress.NONE);
            RealImage b = Convolution.apply(image, full, mode, Progress.NONE);
            for (int i = 0; i < a.data().length; i++) {
                assertEquals(b.data()[i], a.data()[i], 1e-9);
            }
        }
    }

    @Test
    void laplacianFindsOnlyIsolatedPoint() {
        GrayImage image = flat(20, 20, 50);
        image.set(7, 11, 200);
        GrayImage points = PointDetector.detect(image, 90, EdgeMode.REFLECT, Progress.NONE).gray();
        assertEquals(1, count(points));
        assertEquals(255, points.get(7, 11));
    }

    @Test
    void horizontalMaskFindsHorizontalLineOnly() {
        GrayImage image = flat(30, 30, 20);
        for (int x = 0; x < 30; x++) {
            image.set(x, 15, 200);
        }
        GrayImage horizontal = LineDetector.detect(image, LineDetector.Direction.HORIZONTAL, false, 50,
                EdgeMode.REFLECT, Progress.NONE).gray();
        GrayImage vertical = LineDetector.detect(image, LineDetector.Direction.VERTICAL, false, 50,
                EdgeMode.REFLECT, Progress.NONE).gray();
        assertEquals(30, count(horizontal));
        assertEquals(255, horizontal.get(10, 15));
        assertTrue(count(vertical) <= count(horizontal));
        assertEquals(0, vertical.get(10, 15));
    }

    @Test
    void gradientAngleOfVerticalStepPointsRight() {
        int w = 20;
        int h = 10;
        RealImage image = new RealImage(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.data()[y * w + x] = x < 10 ? 50 : 200;
            }
        }
        for (EdgeDetector.Operator op : EdgeDetector.Operator.values()) {
            EdgeDetector.Gradient g = EdgeDetector.gradient(image, op, EdgeMode.CLAMP, Progress.NONE);
            int x = op == EdgeDetector.Operator.ROBERTS ? 9 : 10;
            assertTrue(g.magnitude().get(x, 5) > 0, op.toString());
            assertEquals(0.0, g.angle().get(x, 5), 1e-9, op.toString());
        }
    }

    @Test
    void gradientAngleOfHorizontalStepPointsDown() {
        int w = 10;
        int h = 20;
        RealImage image = new RealImage(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.data()[y * w + x] = y < 10 ? 50 : 200;
            }
        }
        EdgeDetector.Gradient g = EdgeDetector.gradient(image, EdgeDetector.Operator.SOBEL, EdgeMode.CLAMP, Progress.NONE);
        assertEquals(Math.PI / 2, g.angle().get(5, 10), 1e-9);
    }

    @Test
    void cannyGivesThinEdge() {
        int w = 40;
        int h = 30;
        int[] data = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                data[y * w + x] = x < 20 ? 40 : 210;
            }
        }
        GrayImage edges = Canny.detect(new GrayImage(w, h, data), 1.5, 25, 0.4, false, EdgeMode.REFLECT,
                Progress.NONE).gray();
        for (int y = 5; y < h - 5; y++) {
            int inRow = 0;
            for (int x = 0; x < w; x++) {
                if (edges.get(x, y) > 0) {
                    inRow++;
                    assertTrue(x >= 18 && x <= 21, "край в столбце " + x);
                }
            }
            assertEquals(1, inRow, "строка " + y);
        }
    }

    @Test
    void parallelResultEqualsSequentialDefinition() {
        Random random = new Random(9);
        int w = 300;
        int h = 257;
        RealImage image = new RealImage(w, h);
        for (int i = 0; i < image.data().length; i++) {
            image.data()[i] = random.nextInt(256);
        }
        RealImage parallel = Convolution.apply(image, Kernel.SOBEL_X, EdgeMode.REFLECT, Progress.NONE);
        for (int y = 0; y < h; y += 17) {
            for (int x = 0; x < w; x += 13) {
                double expected = 0;
                for (int j = -1; j <= 1; j++) {
                    for (int i = -1; i <= 1; i++) {
                        expected += Kernel.SOBEL_X.at(i + 1, j + 1) * image.get(x + i, y + j, EdgeMode.REFLECT);
                    }
                }
                assertEquals(expected, parallel.get(x, y), 1e-9);
            }
        }
    }
}
