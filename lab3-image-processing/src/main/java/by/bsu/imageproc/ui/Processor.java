package by.bsu.imageproc.ui;

import by.bsu.imageproc.buffer.GrayImage;
import by.bsu.imageproc.processing.AdaptiveMedian;
import by.bsu.imageproc.processing.AdaptiveThreshold;
import by.bsu.imageproc.processing.GlobalThreshold;
import by.bsu.imageproc.processing.Histogram;
import by.bsu.imageproc.processing.LinearFilter;
import by.bsu.imageproc.processing.Morphology;
import by.bsu.imageproc.processing.OrderFilter;
import by.bsu.imageproc.processing.Progress;
import by.bsu.imageproc.processing.Quality;
import by.bsu.imageproc.processing.Result;
import by.bsu.imageproc.processing.Statistic;
import by.bsu.imageproc.processing.WindowShape;

import java.util.Locale;

public final class Processor {

    private Processor() {
    }

    public static Result run(GrayImage image, Settings s, Progress progress) {
        if (s.method().isThreshold()) {
            return threshold(image, s, progress);
        }
        WindowShape.Window window = s.shape().of(s.filterRadius());
        String windowText = String.format(Locale.ROOT, "окно «%s» r = %d (%d пикселей)",
                s.shape(), window.radius(), window.size());
        GrayImage filtered;
        String info;
        switch (s.method()) {
            case ADAPTIVE_MEDIAN -> {
                long[] stats = new long[2];
                filtered = AdaptiveMedian.filter(image, s.maxRadius(), s.edgeMode(), progress, stats);
                info = AdaptiveMedian.describe(s.maxRadius(), stats, image.data().length);
            }
            case MORPHOLOGY -> {
                filtered = Morphology.apply(image, s.operation(), window, s.edgeMode(), progress);
                info = s.operation() + ", структурирующий элемент: " + windowText;
                if (s.operation().isDifference() && s.stretch()) {
                    int max = max(filtered);
                    filtered = Quality.stretch(filtered);
                    info += String.format(Locale.ROOT, ". Результат растянут на 0…255 (было 0…%d)", max);
                }
            }
            case LINEAR -> {
                filtered = LinearFilter.apply(image, s.linearKind(), s.filterRadius(), s.sigma(), s.edgeMode(), progress);
                info = s.linearKind() + (s.linearKind() == LinearFilter.Kind.BOX
                        ? String.format(Locale.ROOT, ", окно %d×%d", 2 * s.filterRadius() + 1, 2 * s.filterRadius() + 1)
                        : String.format(Locale.ROOT, ", σ = %.1f, ядро %d×%d", s.sigma(),
                        2 * Math.max(1, (int) Math.ceil(3 * s.sigma())) + 1, 2 * Math.max(1, (int) Math.ceil(3 * s.sigma())) + 1));
            }
            default -> {
                Statistic statistic = statistic(s);
                progress.stages(1);
                filtered = s.naive()
                        ? OrderFilter.naive(image, window, statistic, s.edgeMode(), progress)
                        : OrderFilter.huang(image, window, statistic, s.edgeMode(), progress);
                info = "Результат — " + statistic.title() + ", " + windowText
                        + (s.naive() ? ". Наивный алгоритм: сортировка окна для каждого пикселя"
                        : ". Алгоритм Хуанга: скользящая гистограмма окна");
            }
        }
        if (s.binarize()) {
            GlobalThreshold.Otsu otsu = GlobalThreshold.otsu(Histogram.of(filtered));
            return Result.gray(GlobalThreshold.binarize(filtered, otsu.threshold(), progress), null,
                    info + String.format(Locale.ROOT, ". Затем порог Оцу T = %d", otsu.threshold()));
        }
        return Result.gray(filtered, null, info);
    }

    public static Statistic statistic(Settings s) {
        return switch (s.method()) {
            case MEDIAN -> Statistic.MEDIAN;
            case MIN -> Statistic.MIN;
            case MAX -> Statistic.MAX;
            case MIDPOINT -> Statistic.MIDPOINT;
            case ALPHA_TRIMMED -> Statistic.alphaTrimmed(s.trim());
            case PERCENTILE -> Statistic.percentile(s.percentile() / 100.0);
            default -> throw new IllegalArgumentException(s.method().name());
        };
    }

    private static Result threshold(GrayImage image, Settings s, Progress progress) {
        GrayImage source = image;
        String prefix = "";
        if (s.prefilter() > 0) {
            source = OrderFilter.huang(image, WindowShape.SQUARE.of(s.prefilter()), Statistic.MEDIAN, s.edgeMode(),
                    progress);
            int side = 2 * s.prefilter() + 1;
            prefix = String.format(Locale.ROOT, "Предварительно медиана %d×%d. ", side, side);
        }
        Result result = switch (s.method()) {
            case ITERATIVE -> GlobalThreshold.applyIterative(source, progress);
            case OTSU -> GlobalThreshold.applyOtsu(source, progress);
            case ADAPTIVE_MEAN -> AdaptiveThreshold.meanMinusC(source, s.radius(), s.c(), s.edgeMode(), progress);
            case ADAPTIVE_SAUVOLA -> AdaptiveThreshold.sauvola(source, s.radius(), s.k(), s.edgeMode(), progress);
            default -> throw new IllegalArgumentException(s.method().name());
        };
        return Result.gray(result.gray(), s.prefilter() > 0 ? null : result.threshold(), prefix + result.info());
    }

    private static int max(GrayImage image) {
        int max = 0;
        for (int v : image.data()) {
            max = Math.max(max, v);
        }
        return max;
    }
}
