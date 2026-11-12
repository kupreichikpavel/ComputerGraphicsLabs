package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;

public final class Convolution {

    private Convolution() {
    }

    public static RealImage apply(RealImage src, Kernel kernel, EdgeMode mode, Progress progress) {
        int width = src.width();
        int height = src.height();
        RealImage dst = new RealImage(width, height);
        double[] out = dst.data();
        double[] in = src.data();
        int kw = kernel.width();
        int kh = kernel.height();
        int ax = kernel.anchorX();
        int ay = kernel.anchorY();
        double[] w = kernel.weights();
        Parallel.rows(height, progress, y -> {
            boolean innerRow = y - ay >= 0 && y - ay + kh <= height;
            for (int x = 0; x < width; x++) {
                double acc = 0;
                if (innerRow && x - ax >= 0 && x - ax + kw <= width) {
                    for (int j = 0; j < kh; j++) {
                        int row = (y - ay + j) * width + (x - ax);
                        int k = j * kw;
                        for (int i = 0; i < kw; i++) {
                            acc += w[k + i] * in[row + i];
                        }
                    }
                } else {
                    for (int j = 0; j < kh; j++) {
                        int sy = mode.index(y - ay + j, height);
                        for (int i = 0; i < kw; i++) {
                            acc += w[j * kw + i] * in[sy * width + mode.index(x - ax + i, width)];
                        }
                    }
                }
                out[y * width + x] = acc;
            }
        });
        return dst;
    }

    public static RealImage separable(RealImage src, double[] kernel1d, EdgeMode mode, Progress progress) {
        int width = src.width();
        int height = src.height();
        int r = kernel1d.length / 2;
        double[] in = src.data();
        RealImage horizontal = new RealImage(width, height);
        double[] h = horizontal.data();
        Parallel.rows(height, progress, y -> {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                double acc = 0;
                for (int i = -r; i <= r; i++) {
                    acc += kernel1d[i + r] * in[row + mode.index(x + i, width)];
                }
                h[row + x] = acc;
            }
        });
        progress.nextStage();
        RealImage result = new RealImage(width, height);
        double[] out = result.data();
        Parallel.rows(height, progress, y -> {
            for (int x = 0; x < width; x++) {
                double acc = 0;
                for (int i = -r; i <= r; i++) {
                    acc += kernel1d[i + r] * h[mode.index(y + i, height) * width + x];
                }
                out[y * width + x] = acc;
            }
        });
        return result;
    }

    public static double[] gaussian1d(double sigma) {
        int r = Math.max(1, (int) Math.ceil(3 * sigma));
        double[] k = new double[2 * r + 1];
        double sum = 0;
        for (int i = -r; i <= r; i++) {
            k[i + r] = Math.exp(-(i * i) / (2 * sigma * sigma));
            sum += k[i + r];
        }
        for (int i = 0; i < k.length; i++) {
            k[i] /= sum;
        }
        return k;
    }
}
