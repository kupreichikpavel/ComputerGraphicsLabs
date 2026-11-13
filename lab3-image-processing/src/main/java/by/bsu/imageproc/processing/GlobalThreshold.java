package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.GrayImage;

import java.util.Locale;

public final class GlobalThreshold {

    private static final double MAX_DELTA = 0.5;
    private static final int MAX_ITERATIONS = 1000;

    private GlobalThreshold() {
    }

    public record Iterative(int threshold, int iterations) {
    }

    public record Otsu(int threshold, double betweenClassVariance, double separability) {
    }

    public static Iterative iterative(long[] histogram) {
        long total = 0;
        double sum = 0;
        for (int i = 0; i < Histogram.LEVELS; i++) {
            total += histogram[i];
            sum += (double) i * histogram[i];
        }
        double t = sum / Math.max(1, total);
        int iterations = 0;
        while (iterations < MAX_ITERATIONS) {
            iterations++;
            long count1 = 0;
            long count2 = 0;
            double sum1 = 0;
            double sum2 = 0;
            for (int i = 0; i < Histogram.LEVELS; i++) {
                if (i > t) {
                    count1 += histogram[i];
                    sum1 += (double) i * histogram[i];
                } else {
                    count2 += histogram[i];
                    sum2 += (double) i * histogram[i];
                }
            }
            double m1 = count1 == 0 ? t : sum1 / count1;
            double m2 = count2 == 0 ? t : sum2 / count2;
            double next = (m1 + m2) / 2;
            boolean converged = Math.abs(next - t) < MAX_DELTA;
            t = next;
            if (converged) {
                break;
            }
        }
        return new Iterative((int) Math.round(t), iterations);
    }

    public static Otsu otsu(long[] histogram) {
        long total = 0;
        for (long count : histogram) {
            total += count;
        }
        double[] p = new double[Histogram.LEVELS];
        double globalMean = 0;
        for (int i = 0; i < Histogram.LEVELS; i++) {
            p[i] = (double) histogram[i] / Math.max(1, total);
            globalMean += i * p[i];
        }
        double globalVariance = 0;
        for (int i = 0; i < Histogram.LEVELS; i++) {
            globalVariance += (i - globalMean) * (i - globalMean) * p[i];
        }

        double omega = 0;
        double mu = 0;
        double best = -1;
        int bestSum = 0;
        int bestCount = 0;
        for (int k = 0; k < Histogram.LEVELS - 1; k++) {
            omega += p[k];
            mu += k * p[k];
            if (omega <= 0 || omega >= 1) {
                continue;
            }
            double numerator = globalMean * omega - mu;
            double sigmaB = numerator * numerator / (omega * (1 - omega));
            if (sigmaB > best + 1e-12) {
                best = sigmaB;
                bestSum = k;
                bestCount = 1;
            } else if (Math.abs(sigmaB - best) <= 1e-12) {
                bestSum += k;
                bestCount++;
            }
        }
        int threshold = bestCount == 0 ? (int) Math.round(globalMean) : Math.round((float) bestSum / bestCount);
        double variance = Math.max(0, best);
        double separability = globalVariance == 0 ? 0 : variance / globalVariance;
        return new Otsu(threshold, variance, separability);
    }

    public static GrayImage binarize(GrayImage image, int threshold, Progress progress) {
        int width = image.width();
        int[] src = image.data();
        int[] dst = new int[src.length];
        Parallel.rows(image.height(), progress, y -> {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                dst[row + x] = src[row + x] > threshold ? 255 : 0;
            }
        });
        return new GrayImage(width, image.height(), dst);
    }

    public static Result applyIterative(GrayImage image, Progress progress) {
        Iterative t = iterative(Histogram.of(image));
        return Result.gray(binarize(image, t.threshold(), progress), t.threshold(),
                String.format(Locale.ROOT, "Порог T = %d, итераций: %d", t.threshold(), t.iterations()));
    }

    public static Result applyOtsu(GrayImage image, Progress progress) {
        Otsu t = otsu(Histogram.of(image));
        return Result.gray(binarize(image, t.threshold(), progress), t.threshold(),
                String.format(Locale.ROOT, "Порог Оцу T = %d, межклассовая дисперсия σ²B = %.1f, "
                        + "показатель разделимости η = %.3f", t.threshold(), t.betweenClassVariance(), t.separability()));
    }
}
