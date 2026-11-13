package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;

import java.util.Arrays;

public final class OrderFilter {

    public enum Rank {
        MEDIAN("медиана"),
        MIN("минимум"),
        MAX("максимум");

        private final String title;

        Rank(String title) {
            this.title = title;
        }

        int index(int n) {
            return switch (this) {
                case MEDIAN -> n / 2;
                case MIN -> 0;
                case MAX -> n - 1;
            };
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private OrderFilter() {
    }

    public static GrayImage apply(GrayImage image, WindowShape.Window window, Rank rank, EdgeMode mode,
                                  Progress progress) {
        int width = image.width();
        int height = image.height();
        int n = window.size();
        int[] dx = window.dx();
        int[] dy = window.dy();
        int k = rank.index(n);
        int[] dst = new int[width * height];
        Parallel.rows(height, progress, y -> {
            int[] values = new int[n];
            for (int x = 0; x < width; x++) {
                for (int i = 0; i < n; i++) {
                    values[i] = image.get(x + dx[i], y + dy[i], mode);
                }
                dst[y * width + x] = select(values, rank, k);
            }
        });
        return new GrayImage(width, height, dst);
    }

    private static int select(int[] values, Rank rank, int k) {
        if (rank == Rank.MEDIAN) {
            Arrays.sort(values);
            return values[k];
        }
        int best = values[0];
        for (int v : values) {
            best = rank == Rank.MIN ? Math.min(best, v) : Math.max(best, v);
        }
        return best;
    }
}
