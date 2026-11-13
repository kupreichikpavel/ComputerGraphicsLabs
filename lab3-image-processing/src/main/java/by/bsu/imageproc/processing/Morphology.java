package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;

public final class Morphology {

    public enum Operation {
        EROSION("Эрозия (min)", 1),
        DILATION("Дилатация (max)", 1),
        OPENING("Открытие: дилатация(эрозия)", 2),
        CLOSING("Закрытие: эрозия(дилатация)", 2),
        GRADIENT("Морфологический градиент: дилатация − эрозия", 2),
        TOP_HAT("Top-Hat: f − открытие", 2),
        BLACK_HAT("Black-Hat: закрытие − f", 2);

        private final String title;
        private final int stages;

        Operation(String title, int stages) {
            this.title = title;
            this.stages = stages;
        }

        public boolean isDifference() {
            return this == GRADIENT || this == TOP_HAT || this == BLACK_HAT;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private Morphology() {
    }

    public static GrayImage erode(GrayImage image, WindowShape.Window window, EdgeMode mode, Progress progress) {
        return OrderFilter.huang(image, window, Statistic.MIN, mode, progress);
    }

    public static GrayImage dilate(GrayImage image, WindowShape.Window window, EdgeMode mode, Progress progress) {
        return OrderFilter.huang(image, window, Statistic.MAX, mode, progress);
    }

    public static GrayImage apply(GrayImage f, Operation operation, WindowShape.Window window, EdgeMode mode,
                                  Progress progress) {
        progress.stages(operation.stages);
        return switch (operation) {
            case EROSION -> erode(f, window, mode, progress);
            case DILATION -> dilate(f, window, mode, progress);
            case OPENING -> dilate(next(erode(f, window, mode, progress), progress), window, mode, progress);
            case CLOSING -> erode(next(dilate(f, window, mode, progress), progress), window, mode, progress);
            case GRADIENT -> {
                GrayImage eroded = next(erode(f, window, mode, progress), progress);
                yield difference(dilate(f, window, mode, progress), eroded);
            }
            case TOP_HAT -> difference(f, dilate(next(erode(f, window, mode, progress), progress), window, mode, progress));
            case BLACK_HAT -> difference(erode(next(dilate(f, window, mode, progress), progress), window, mode, progress), f);
        };
    }

    private static GrayImage next(GrayImage image, Progress progress) {
        progress.nextStage();
        return image;
    }

    public static GrayImage difference(GrayImage a, GrayImage b) {
        int[] x = a.data();
        int[] y = b.data();
        int[] out = new int[x.length];
        for (int i = 0; i < x.length; i++) {
            out[i] = Math.max(0, x[i] - y[i]);
        }
        return new GrayImage(a.width(), a.height(), out);
    }
}
