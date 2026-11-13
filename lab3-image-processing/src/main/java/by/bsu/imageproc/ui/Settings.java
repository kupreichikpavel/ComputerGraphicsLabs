package by.bsu.imageproc.ui;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.processing.Morphology;
import by.bsu.imageproc.processing.WindowShape;

public record Settings(
        Method method,
        EdgeMode edgeMode,
        int radius,
        double k,
        WindowShape shape,
        int filterRadius,
        Morphology.Operation operation,
        boolean binarize
) {
}
