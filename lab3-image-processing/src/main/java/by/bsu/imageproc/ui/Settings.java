package by.bsu.imageproc.ui;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.processing.LinearFilter;
import by.bsu.imageproc.processing.Morphology;
import by.bsu.imageproc.processing.Noise;
import by.bsu.imageproc.processing.WindowShape;

public record Settings(
        Method method,
        EdgeMode edgeMode,
        int radius,
        double c,
        double k,
        int prefilter,
        WindowShape shape,
        int filterRadius,
        double trim,
        double percentile,
        int maxRadius,
        Morphology.Operation operation,
        boolean stretch,
        LinearFilter.Kind linearKind,
        double sigma,
        boolean naive,
        boolean binarize,
        Noise.Kind noise,
        double noiseAmount
) {
}
