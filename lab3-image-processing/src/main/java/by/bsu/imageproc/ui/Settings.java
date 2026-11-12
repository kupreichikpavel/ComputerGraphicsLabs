package by.bsu.imageproc.ui;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.processing.EdgeDetector;
import by.bsu.imageproc.processing.LineDetector;

public record Settings(
        Method method,
        EdgeMode edgeMode,
        int radius,
        double c,
        double k,
        double percent,
        LineDetector.Direction direction,
        boolean darkLines,
        EdgeDetector.Operator operator,
        EdgeDetector.Output output,
        double sigma,
        double cannySigma,
        double highPercent,
        double lowRatio,
        boolean cannyColor
) {
}
