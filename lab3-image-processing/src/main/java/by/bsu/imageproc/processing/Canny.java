package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;

import java.util.Locale;

public final class Canny {

    private static final int STRONG = 2;
    private static final int WEAK = 1;

    private Canny() {
    }

    public static Result detect(GrayImage image, double sigma, double highPercent, double lowRatio,
                                boolean colorDirections, EdgeMode mode, Progress progress) {
        progress.stages(6);
        RealImage smoothed = Convolution.separable(RealImage.of(image), Convolution.gaussian1d(sigma), mode, progress);
        progress.nextStage();
        EdgeDetector.Gradient g = EdgeDetector.gradient(smoothed, EdgeDetector.Operator.SOBEL, mode, progress);

        RealImage suppressed = nonMaximumSuppression(g, mode, progress);
        progress.nextStage();

        double high = g.magnitude().max() * highPercent / 100.0;
        double low = high * lowRatio;
        boolean[] edges = hysteresis(suppressed, high, low);
        progress.nextStage();

        int width = image.width();
        int height = image.height();
        GrayImage binary = new GrayImage(width, height);
        int count = 0;
        for (int i = 0; i < edges.length; i++) {
            if (edges[i]) {
                binary.data()[i] = 255;
                count++;
            }
        }
        String info = String.format(Locale.ROOT,
                "Гаусс σ = %.1f (две 1D-свёртки) → Собел → подавление немаксимумов с интерполяцией "
                        + "→ гистерезис: T_high = %.0f, T_low = %.0f; пикселей края: %d",
                sigma, high, low, count);
        if (colorDirections) {
            return Result.color(DirectionColors.map(g.magnitude(), g.angle(), edges), binary, info);
        }
        return Result.gray(binary, null, info);
    }

    static RealImage nonMaximumSuppression(EdgeDetector.Gradient g, EdgeMode mode, Progress progress) {
        RealImage magnitude = g.magnitude();
        int width = magnitude.width();
        int height = magnitude.height();
        RealImage out = new RealImage(width, height);
        double[] m = magnitude.data();
        double[] gx = g.gx().data();
        double[] gy = g.gy().data();
        double[] o = out.data();
        Parallel.rows(height, progress, y -> {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                double value = m[i];
                if (value == 0) {
                    continue;
                }
                double length = Math.hypot(gx[i], gy[i]);
                if (length == 0) {
                    continue;
                }
                double dx = gx[i] / length;
                double dy = gy[i] / length;
                double ahead = bilinear(magnitude, x + dx, y + dy, mode);
                double behind = bilinear(magnitude, x - dx, y - dy, mode);
                if (value >= ahead && value > behind) {
                    o[i] = value;
                }
            }
        });
        return out;
    }

    static double bilinear(RealImage image, double x, double y, EdgeMode mode) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;
        double a = image.get(x0, y0, mode);
        double b = image.get(x0 + 1, y0, mode);
        double c = image.get(x0, y0 + 1, mode);
        double d = image.get(x0 + 1, y0 + 1, mode);
        return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy;
    }

    static boolean[] hysteresis(RealImage suppressed, double high, double low) {
        int width = suppressed.width();
        int height = suppressed.height();
        double[] m = suppressed.data();
        byte[] state = new byte[m.length];
        int[] stack = new int[m.length];
        int top = 0;
        for (int i = 0; i < m.length; i++) {
            if (m[i] >= high && m[i] > 0) {
                state[i] = STRONG;
                stack[top++] = i;
            } else if (m[i] >= low && m[i] > 0) {
                state[i] = WEAK;
            }
        }
        while (top > 0) {
            int i = stack[--top];
            int x = i % width;
            int y = i / width;
            for (int dy = -1; dy <= 1; dy++) {
                int ny = y + dy;
                if (ny < 0 || ny >= height) {
                    continue;
                }
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx;
                    if (nx < 0 || nx >= width) {
                        continue;
                    }
                    int n = ny * width + nx;
                    if (state[n] == WEAK) {
                        state[n] = STRONG;
                        stack[top++] = n;
                    }
                }
            }
        }
        boolean[] edges = new boolean[m.length];
        for (int i = 0; i < m.length; i++) {
            edges[i] = state[i] == STRONG;
        }
        return edges;
    }
}
