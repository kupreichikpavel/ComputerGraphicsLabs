package by.bsu.imageproc.ui;

import by.bsu.imageproc.buffer.GrayImage;
import by.bsu.imageproc.processing.AdaptiveThreshold;
import by.bsu.imageproc.processing.GlobalThreshold;
import by.bsu.imageproc.processing.Histogram;
import by.bsu.imageproc.processing.Morphology;
import by.bsu.imageproc.processing.OrderFilter;
import by.bsu.imageproc.processing.Progress;
import by.bsu.imageproc.processing.Result;
import by.bsu.imageproc.processing.WindowShape;

import java.util.Locale;

public final class Processor {

    private Processor() {
    }

    public static Result run(GrayImage image, Settings s, Progress progress) {
        switch (s.method()) {
            case ITERATIVE:
                return GlobalThreshold.applyIterative(image, progress);
            case OTSU:
                return GlobalThreshold.applyOtsu(image, progress);
            case ADAPTIVE_SAUVOLA:
                return AdaptiveThreshold.sauvola(image, s.radius(), s.k(), s.edgeMode(), progress);
            default:
                break;
        }
        WindowShape.Window window = s.shape().of(s.filterRadius());
        String windowText = String.format(Locale.ROOT, "окно «%s» %d×%d (%d пикселей)",
                s.shape(), window.side(), window.side(), window.size());
        GrayImage filtered;
        String info;
        if (s.method() == Method.MORPHOLOGY) {
            GrayImage raw = Morphology.apply(image, s.operation(), window, s.edgeMode(), progress);
            filtered = Morphology.stretch(raw);
            info = String.format(Locale.ROOT, "%s, %s. Результат растянут на 0…255 (было 0…%d)",
                    s.operation(), windowText, max(raw));
        } else {
            OrderFilter.Rank rank = switch (s.method()) {
                case MIN -> OrderFilter.Rank.MIN;
                case MAX -> OrderFilter.Rank.MAX;
                default -> OrderFilter.Rank.MEDIAN;
            };
            progress.stages(1);
            filtered = OrderFilter.apply(image, window, rank, s.edgeMode(), progress);
            info = "Результат — " + rank + " отсортированных значений, " + windowText;
        }
        if (s.binarize()) {
            GlobalThreshold.Otsu otsu = GlobalThreshold.otsu(Histogram.of(filtered));
            return Result.gray(GlobalThreshold.binarize(filtered, otsu.threshold(), progress), null,
                    info + String.format(Locale.ROOT, ". Затем порог Оцу T = %d", otsu.threshold()));
        }
        return Result.gray(filtered, null, info);
    }

    private static int max(GrayImage image) {
        int max = 0;
        for (int v : image.data()) {
            max = Math.max(max, v);
        }
        return max;
    }
}
