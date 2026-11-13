package by.bsu.imageproc.processing;

public record Statistic(Kind kind, double parameter) {

    public enum Kind {
        MEDIAN, MIN, MAX, MIDPOINT, ALPHA_TRIMMED, PERCENTILE
    }

    public static final Statistic MEDIAN = new Statistic(Kind.MEDIAN, 0);
    public static final Statistic MIN = new Statistic(Kind.MIN, 0);
    public static final Statistic MAX = new Statistic(Kind.MAX, 0);
    public static final Statistic MIDPOINT = new Statistic(Kind.MIDPOINT, 0);

    public static Statistic alphaTrimmed(double trimPerSide) {
        return new Statistic(Kind.ALPHA_TRIMMED, Math.max(0, Math.min(0.49, trimPerSide)));
    }

    public static Statistic percentile(double p) {
        return new Statistic(Kind.PERCENTILE, Math.max(0, Math.min(1, p)));
    }

    public int trimmed(int n) {
        return Math.min((n - 1) / 2, (int) Math.floor(parameter * n));
    }

    public int rank(int n) {
        return switch (kind) {
            case MEDIAN -> n / 2;
            case MIN -> 0;
            case MAX -> n - 1;
            case PERCENTILE -> (int) Math.round(parameter * (n - 1));
            default -> throw new IllegalStateException();
        };
    }

    public int fromSorted(int[] sorted, int n) {
        return switch (kind) {
            case MIDPOINT -> (sorted[0] + sorted[n - 1] + 1) >> 1;
            case ALPHA_TRIMMED -> {
                int d = trimmed(n);
                long sum = 0;
                for (int i = d; i < n - d; i++) {
                    sum += sorted[i];
                }
                yield roundedMean(sum, n - 2 * d);
            }
            default -> sorted[rank(n)];
        };
    }

    static int roundedMean(long sum, int count) {
        return (int) ((sum + count / 2) / count);
    }

    public String title() {
        return switch (kind) {
            case MEDIAN -> "медиана";
            case MIN -> "минимум";
            case MAX -> "максимум";
            case MIDPOINT -> "средняя точка (min + max) / 2";
            case ALPHA_TRIMMED -> String.format(java.util.Locale.ROOT, "усечённое среднее, отброшено %.0f%% с каждой стороны", parameter * 100);
            case PERCENTILE -> String.format(java.util.Locale.ROOT, "%.0f-й процентиль", parameter * 100);
        };
    }
}
