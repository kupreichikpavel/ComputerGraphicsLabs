package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;

import java.util.Locale;

public final class EdgeDetector {

    public enum Operator {
        ROBERTS("Робертс (2×2)"),
        PREWITT("Превитт (3×3)"),
        SOBEL("Собел (3×3)");

        private final String title;

        Operator(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum Output {
        MAGNITUDE("Модуль градиента"),
        EDGES("Края (модуль ≥ порога)"),
        DIRECTIONS("Карта направлений (цвет = угол)");

        private final String title;

        Output(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public record Gradient(RealImage gx, RealImage gy, RealImage magnitude, RealImage angle) {
    }

    private EdgeDetector() {
    }

    public static Gradient gradient(RealImage image, Operator operator, EdgeMode mode, Progress progress) {
        Kernel kx;
        Kernel ky;
        switch (operator) {
            case ROBERTS -> {
                kx = Kernel.ROBERTS_DIAGONAL;
                ky = Kernel.ROBERTS_ANTIDIAGONAL;
            }
            case PREWITT -> {
                kx = Kernel.PREWITT_X;
                ky = Kernel.PREWITT_Y;
            }
            default -> {
                kx = Kernel.SOBEL_X;
                ky = Kernel.SOBEL_Y;
            }
        }
        RealImage g1 = Convolution.apply(image, kx, mode, progress);
        progress.nextStage();
        RealImage g2 = Convolution.apply(image, ky, mode, progress);
        progress.nextStage();

        RealImage gx;
        RealImage gy;
        if (operator == Operator.ROBERTS) {
            gx = new RealImage(image.width(), image.height());
            gy = new RealImage(image.width(), image.height());
            for (int i = 0; i < gx.data().length; i++) {
                double d1 = g1.data()[i];
                double d2 = g2.data()[i];
                gx.data()[i] = (d1 - d2) / 2;
                gy.data()[i] = (d1 + d2) / 2;
            }
        } else {
            gx = g1;
            gy = g2;
        }

        RealImage magnitude = new RealImage(image.width(), image.height());
        RealImage angle = new RealImage(image.width(), image.height());
        double[] m = magnitude.data();
        double[] a = angle.data();
        double[] x = gx.data();
        double[] y = gy.data();
        double[] d1 = g1.data();
        double[] d2 = g2.data();
        for (int i = 0; i < m.length; i++) {
            m[i] = Math.sqrt(d1[i] * d1[i] + d2[i] * d2[i]);
            a[i] = Math.atan2(y[i], x[i]);
        }
        return new Gradient(gx, gy, magnitude, angle);
    }

    public static Result detect(GrayImage image, Operator operator, Output output, double sigma,
                                double thresholdPercent, EdgeMode mode, Progress progress) {
        boolean smooth = sigma > 0;
        progress.stages(smooth ? 4 : 2);
        RealImage source = RealImage.of(image);
        if (smooth) {
            source = Convolution.separable(source, Convolution.gaussian1d(sigma), mode, progress);
            progress.nextStage();
        }
        Gradient g = gradient(source, operator, mode, progress);
        String prefix = String.format(Locale.ROOT, "%s%s", operator,
                smooth ? String.format(Locale.ROOT, ", предварительный Гаусс σ = %.1f (две 1D-свёртки)", sigma) : "");
        return switch (output) {
            case MAGNITUDE -> Result.gray(g.magnitude().normalized(), null, String.format(Locale.ROOT,
                    "%s; |∇f| = √(Gx² + Gy²), max = %.0f, нормировано в 0–255", prefix, g.magnitude().max()));
            case EDGES -> {
                double t = g.magnitude().max() * thresholdPercent / 100.0;
                yield Result.gray(g.magnitude().thresholded(t), null, String.format(Locale.ROOT,
                        "%s; |∇f| ≥ T = %.0f (%.0f%% от max)", prefix, t, thresholdPercent));
            }
            case DIRECTIONS -> Result.color(DirectionColors.map(g.magnitude(), g.angle(), null),
                    g.magnitude().normalized(), String.format(Locale.ROOT,
                            "%s; угол α = atan2(Gy, Gx) для каждого пикселя → оттенок, яркость → |∇f|", prefix));
        };
    }
}
