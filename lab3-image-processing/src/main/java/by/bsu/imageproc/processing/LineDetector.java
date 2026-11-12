package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.ColorImage;
import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;

import java.util.Locale;

public final class LineDetector {

    public enum Direction {
        HORIZONTAL("Горизонтальные", Kernel.LINE_HORIZONTAL, ColorImage.rgb(255, 80, 80)),
        PLUS_45("+45°", Kernel.LINE_PLUS_45, ColorImage.rgb(80, 220, 80)),
        VERTICAL("Вертикальные", Kernel.LINE_VERTICAL, ColorImage.rgb(80, 140, 255)),
        MINUS_45("−45°", Kernel.LINE_MINUS_45, ColorImage.rgb(255, 210, 60)),
        ALL("Все направления (цветом)", null, 0);

        private final String title;
        private final Kernel kernel;
        private final int color;

        Direction(String title, Kernel kernel, int color) {
            this.title = title;
            this.kernel = kernel;
            this.color = color;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private LineDetector() {
    }

    public static Result detect(GrayImage image, Direction direction, boolean darkLines, double thresholdPercent,
                                EdgeMode mode, Progress progress) {
        RealImage source = RealImage.of(image);
        double sign = darkLines ? -1 : 1;
        if (direction != Direction.ALL) {
            RealImage response = Convolution.apply(source, direction.kernel, mode, progress);
            double[] r = response.data();
            for (int i = 0; i < r.length; i++) {
                r[i] *= sign;
            }
            double threshold = response.max() * thresholdPercent / 100.0;
            return Result.gray(response.thresholded(threshold), null, String.format(Locale.ROOT,
                    "Маска «%s», R ≥ T = %.0f (%.0f%% от max R = %.0f)",
                    direction.title, threshold, thresholdPercent, response.max()));
        }

        Direction[] directions = {Direction.HORIZONTAL, Direction.PLUS_45, Direction.VERTICAL, Direction.MINUS_45};
        progress.stages(directions.length + 1);
        RealImage[] responses = new RealImage[directions.length];
        double max = 0;
        for (int d = 0; d < directions.length; d++) {
            responses[d] = Convolution.apply(source, directions[d].kernel, mode, progress);
            double[] r = responses[d].data();
            for (int i = 0; i < r.length; i++) {
                r[i] *= sign;
                max = Math.max(max, r[i]);
            }
            progress.nextStage();
        }
        double threshold = max * thresholdPercent / 100.0;
        int width = image.width();
        ColorImage color = new ColorImage(width, image.height());
        GrayImage binary = new GrayImage(width, image.height());
        int[] counts = new int[directions.length];
        int[] colorData = color.data();
        int[] binaryData = binary.data();
        int black = ColorImage.rgb(0, 0, 0);
        for (int i = 0; i < colorData.length; i++) {
            int best = 0;
            for (int d = 1; d < directions.length; d++) {
                if (responses[d].data()[i] > responses[best].data()[i]) {
                    best = d;
                }
            }
            if (responses[best].data()[i] >= threshold && responses[best].data()[i] > 0) {
                colorData[i] = directions[best].color;
                binaryData[i] = 255;
                counts[best]++;
            } else {
                colorData[i] = black;
            }
        }
        progress.nextStage();
        return Result.color(color, binary, String.format(Locale.ROOT,
                "4 маски, у каждого пикселя выбирается маска с max R; T = %.0f. Пикселей: "
                        + "горизонт. %d (красный), +45° %d (зелёный), вертик. %d (синий), −45° %d (жёлтый)",
                threshold, counts[0], counts[1], counts[2], counts[3]));
    }
}
