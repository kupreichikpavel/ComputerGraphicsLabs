package by.bsu.imageproc.ui;

import by.bsu.imageproc.buffer.GrayImage;
import by.bsu.imageproc.processing.AdaptiveThreshold;
import by.bsu.imageproc.processing.Canny;
import by.bsu.imageproc.processing.EdgeDetector;
import by.bsu.imageproc.processing.GlobalThreshold;
import by.bsu.imageproc.processing.LineDetector;
import by.bsu.imageproc.processing.PointDetector;
import by.bsu.imageproc.processing.Progress;
import by.bsu.imageproc.processing.Result;

public final class Processor {

    private Processor() {
    }

    public static Result run(GrayImage image, Settings s, Progress progress) {
        return switch (s.method()) {
            case ITERATIVE -> GlobalThreshold.applyIterative(image, progress);
            case OTSU -> GlobalThreshold.applyOtsu(image, progress);
            case ADAPTIVE_MEAN -> AdaptiveThreshold.meanMinusC(image, s.radius(), s.c(), s.edgeMode(), progress);
            case ADAPTIVE_SAUVOLA -> AdaptiveThreshold.sauvola(image, s.radius(), s.k(), s.edgeMode(), progress);
            case POINTS -> PointDetector.detect(image, s.percent(), s.edgeMode(), progress);
            case LINES -> LineDetector.detect(image, s.direction(), s.darkLines(), s.percent(), s.edgeMode(), progress);
            case EDGES -> EdgeDetector.detect(image, s.operator(), s.output(), s.sigma(), s.percent(), s.edgeMode(),
                    progress);
            case CANNY -> Canny.detect(image, s.cannySigma(), s.highPercent(), s.lowRatio(), s.cannyColor(),
                    s.edgeMode(), progress);
        };
    }
}
