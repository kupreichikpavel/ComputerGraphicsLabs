package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;

import java.util.Locale;

public final class PointDetector {

    private PointDetector() {
    }

    public static Result detect(GrayImage image, double thresholdPercent, EdgeMode mode, Progress progress) {
        RealImage response = Convolution.apply(RealImage.of(image), Kernel.LAPLACIAN_POINT, mode, progress);
        double[] r = response.data();
        for (int i = 0; i < r.length; i++) {
            r[i] = Math.abs(r[i]);
        }
        double threshold = response.max() * thresholdPercent / 100.0;
        GrayImage points = response.thresholded(threshold);
        int count = 0;
        for (int v : points.data()) {
            if (v > 0) {
                count++;
            }
        }
        return Result.gray(points, null, String.format(Locale.ROOT,
                "Маска лапласиана 3×3, |R| ≥ T = %.0f (%.0f%% от max |R| = %.0f); найдено пикселей: %d",
                threshold, thresholdPercent, response.max(), count));
    }
}
