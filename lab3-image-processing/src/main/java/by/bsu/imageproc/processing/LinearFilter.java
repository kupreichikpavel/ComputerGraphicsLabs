package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;

import java.util.Arrays;

public final class LinearFilter {

    public enum Kind {
        BOX("Усредняющий (box), разделяемый"),
        GAUSSIAN("Гаусс, разделяемый"),
        GAUSSIAN_2D("Гаусс, полная 2D-свёртка (для сравнения)");

        private final String title;

        Kind(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private LinearFilter() {
    }

    public static double[] box1d(int radius) {
        double[] k = new double[2 * radius + 1];
        Arrays.fill(k, 1.0 / k.length);
        return k;
    }

    public static GrayImage apply(GrayImage image, Kind kind, int radius, double sigma, EdgeMode mode, Progress progress) {
        RealImage src = RealImage.of(image);
        return switch (kind) {
            case BOX -> {
                progress.stages(2);
                yield Convolution.separable(src, box1d(radius), mode, progress).rounded();
            }
            case GAUSSIAN -> {
                progress.stages(2);
                yield Convolution.separable(src, Convolution.gaussian1d(sigma), mode, progress).rounded();
            }
            case GAUSSIAN_2D -> {
                progress.stages(1);
                yield Convolution.apply(src, Kernel.outer(Convolution.gaussian1d(sigma)), mode, progress).rounded();
            }
        };
    }
}
