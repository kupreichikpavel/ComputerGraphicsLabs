package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;

import java.util.Locale;

public final class AdaptiveThreshold {

    public static final double SAUVOLA_R = 128;

    private AdaptiveThreshold() {
    }

    public static Result sauvola(GrayImage image, int radius, double k, EdgeMode mode, Progress progress) {
        progress.stages(2);
        IntegralImage sat = IntegralImage.build(image, radius, mode);
        progress.nextStage();
        int width = image.width();
        int area = sat.windowArea();
        int[] src = image.data();
        int[] dst = new int[src.length];
        Parallel.rows(image.height(), progress, y -> {
            for (int x = 0; x < width; x++) {
                double mean = (double) sat.windowSum(x, y) / area;
                double meanSquares = (double) sat.windowSumSquares(x, y) / area;
                double std = Math.sqrt(Math.max(0, meanSquares - mean * mean));
                double t = mean * (1 + k * (std / SAUVOLA_R - 1));
                dst[y * width + x] = src[y * width + x] > t ? 255 : 0;
            }
        });
        int size = 2 * radius + 1;
        return Result.gray(new GrayImage(width, image.height(), dst), null,
                String.format(Locale.ROOT, "Окно %d×%d, T(x, y) = m·(1 + %.2f·(s/%.0f − 1)); m и s окна за O(1) через интегральное изображение",
                        size, size, k, SAUVOLA_R));
    }
}
