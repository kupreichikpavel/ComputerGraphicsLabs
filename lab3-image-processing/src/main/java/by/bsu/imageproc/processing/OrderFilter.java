package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;

import java.util.Arrays;

public final class OrderFilter {

    private OrderFilter() {
    }

    public static GrayImage naive(GrayImage image, WindowShape.Window window, Statistic statistic, EdgeMode mode,
                                  Progress progress) {
        int width = image.width();
        int height = image.height();
        int r = window.radius();
        int n = window.size();
        int[] src = image.data();
        int[] dst = new int[src.length];
        int[] xIndex = paddedIndex(width, r, mode);
        Parallel.rows(height, progress, y -> {
            int[] values = new int[n];
            for (int x = 0; x < width; x++) {
                int k = 0;
                for (int dy = -r; dy <= r; dy++) {
                    int row = mode.index(y + dy, height) * width;
                    for (int dx = window.from()[dy + r]; dx <= window.to()[dy + r]; dx++) {
                        values[k++] = src[row + xIndex[x + dx + r]];
                    }
                }
                Arrays.sort(values);
                dst[y * width + x] = statistic.fromSorted(values, n);
            }
        });
        return new GrayImage(width, height, dst);
    }

    public static GrayImage huang(GrayImage image, WindowShape.Window window, Statistic statistic, EdgeMode mode,
                                  Progress progress) {
        int width = image.width();
        int height = image.height();
        int r = window.radius();
        int n = window.size();
        int[] from = window.from();
        int[] to = window.to();
        int[] src = image.data();
        int[] dst = new int[src.length];
        int[] xIndex = paddedIndex(width, r, mode);
        Parallel.rows(height, progress, y -> {
            int[] hist = new int[Histogram.LEVELS];
            int[] rows = new int[2 * r + 1];
            for (int dy = -r; dy <= r; dy++) {
                rows[dy + r] = mode.index(y + dy, height) * width;
            }
            Tracker[] trackers = trackersFor(statistic, n);
            long total = 0;
            for (int j = 0; j < rows.length; j++) {
                for (int dx = from[j]; dx <= to[j]; dx++) {
                    int v = src[rows[j] + xIndex[dx + r]];
                    hist[v]++;
                    total += v;
                    for (Tracker t : trackers) {
                        t.added(v);
                    }
                }
            }
            for (int x = 0; x < width; x++) {
                if (x > 0) {
                    for (int j = 0; j < rows.length; j++) {
                        int out = src[rows[j] + xIndex[x - 1 + from[j] + r]];
                        int in = src[rows[j] + xIndex[x + to[j] + r]];
                        if (out == in) {
                            continue;
                        }
                        hist[out]--;
                        hist[in]++;
                        total += in - out;
                        for (Tracker t : trackers) {
                            t.removed(out);
                            t.added(in);
                        }
                    }
                }
                for (Tracker t : trackers) {
                    t.settle(hist);
                }
                dst[y * width + x] = value(statistic, trackers, total, n);
            }
        });
        return new GrayImage(width, height, dst);
    }

    private static Tracker[] trackersFor(Statistic statistic, int n) {
        return switch (statistic.kind()) {
            case MIDPOINT -> new Tracker[]{new Tracker(0), new Tracker(n - 1)};
            case ALPHA_TRIMMED -> {
                int d = statistic.trimmed(n);
                yield d == 0 ? new Tracker[0] : new Tracker[]{new Tracker(d), new Tracker(n - d)};
            }
            default -> new Tracker[]{new Tracker(statistic.rank(n))};
        };
    }

    private static int value(Statistic statistic, Tracker[] trackers, long total, int n) {
        return switch (statistic.kind()) {
            case MIDPOINT -> (trackers[0].value + trackers[1].value + 1) >> 1;
            case ALPHA_TRIMMED -> {
                if (trackers.length == 0) {
                    yield Statistic.roundedMean(total, n);
                }
                int d = trackers[0].rank;
                yield Statistic.roundedMean(trackers[1].sumOfSmallest() - trackers[0].sumOfSmallest(), n - 2 * d);
            }
            default -> trackers[0].value;
        };
    }

    static int[] paddedIndex(int size, int radius, EdgeMode mode) {
        int[] index = new int[size + 2 * radius];
        for (int i = 0; i < index.length; i++) {
            index[i] = mode.index(i - radius, size);
        }
        return index;
    }

    private static final class Tracker {

        final int rank;
        int value;
        int below;
        long sumBelow;

        Tracker(int rank) {
            this.rank = rank;
        }

        void added(int v) {
            if (v < value) {
                below++;
                sumBelow += v;
            }
        }

        void removed(int v) {
            if (v < value) {
                below--;
                sumBelow -= v;
            }
        }

        void settle(int[] hist) {
            while (below > rank) {
                value--;
                below -= hist[value];
                sumBelow -= (long) hist[value] * value;
            }
            while (below + hist[value] <= rank) {
                below += hist[value];
                sumBelow += (long) hist[value] * value;
                value++;
            }
        }

        long sumOfSmallest() {
            return sumBelow + (long) (rank - below) * value;
        }
    }
}
