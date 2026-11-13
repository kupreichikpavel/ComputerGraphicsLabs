package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public final class AdaptiveMedian {

    private AdaptiveMedian() {
    }

    public static GrayImage filter(GrayImage image, int maxRadius, EdgeMode mode, Progress progress, long[] stats) {
        int width = image.width();
        int height = image.height();
        int smax = Math.max(1, maxRadius);
        int[] src = image.data();
        int[] dst = new int[src.length];
        int[] xIndex = OrderFilter.paddedIndex(width, smax, mode);
        AtomicLong replaced = new AtomicLong();
        AtomicLong grown = new AtomicLong();
        Parallel.rows(height, progress, y -> {
            int[] values = new int[(2 * smax + 1) * (2 * smax + 1)];
            long localReplaced = 0;
            long localGrown = 0;
            for (int x = 0; x < width; x++) {
                int z = src[y * width + x];
                int result = z;
                for (int s = 1; s <= smax; s++) {
                    int n = 0;
                    for (int dy = -s; dy <= s; dy++) {
                        int row = mode.index(y + dy, height) * width;
                        for (int dx = -s; dx <= s; dx++) {
                            values[n++] = src[row + xIndex[x + dx + smax]];
                        }
                    }
                    Arrays.sort(values, 0, n);
                    int zmin = values[0];
                    int zmax = values[n - 1];
                    int zmed = values[n / 2];
                    if (zmin < zmed && zmed < zmax) {
                        result = zmin < z && z < zmax ? z : zmed;
                        break;
                    }
                    if (s == smax) {
                        result = zmed;
                    } else {
                        localGrown++;
                    }
                }
                if (result != z) {
                    localReplaced++;
                }
                dst[y * width + x] = result;
            }
            replaced.addAndGet(localReplaced);
            grown.addAndGet(localGrown);
        });
        if (stats != null && stats.length >= 2) {
            stats[0] = replaced.get();
            stats[1] = grown.get();
        }
        return new GrayImage(width, height, dst);
    }

    public static String describe(int maxRadius, long[] stats, int pixels) {
        int side = 2 * Math.max(1, maxRadius) + 1;
        return String.format(Locale.ROOT, "Окно растёт от 3×3 до %d×%d. Заменено пикселей: %d (%.1f%%), "
                        + "увеличений окна: %d",
                side, side, stats[0], 100.0 * stats[0] / pixels, stats[1]);
    }
}
