package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.buffer.GrayImage;

public final class Morphology {

    public enum Operation {
        GRADIENT("Морфологический градиент: дилатация − эрозия"),
        TOP_HAT("Top-Hat: f − открытие"),
        BLACK_HAT("Black-Hat: закрытие − f");

        private final String title;

        Operation(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private Morphology() {
    }

    public static GrayImage erode(GrayImage image, WindowShape.Window window, EdgeMode mode, Progress progress) {
        return OrderFilter.apply(image, window, OrderFilter.Rank.MIN, mode, progress);
    }

    public static GrayImage dilate(GrayImage image, WindowShape.Window window, EdgeMode mode, Progress progress) {
        return OrderFilter.apply(image, window, OrderFilter.Rank.MAX, mode, progress);
    }

    public static GrayImage apply(GrayImage f, Operation operation, WindowShape.Window window, EdgeMode mode,
                                  Progress progress) {
        progress.stages(2);
        return switch (operation) {
            case GRADIENT -> {
                GrayImage eroded = next(erode(f, window, mode, progress), progress);
                yield difference(dilate(f, window, mode, progress), eroded);
            }
            case TOP_HAT -> difference(f, opening(f, window, mode, progress));
            case BLACK_HAT -> difference(closing(f, window, mode, progress), f);
        };
    }

    public static GrayImage opening(GrayImage f, WindowShape.Window window, EdgeMode mode, Progress progress) {
        return dilate(next(erode(f, window, mode, progress), progress), window, mode, progress);
    }

    public static GrayImage closing(GrayImage f, WindowShape.Window window, EdgeMode mode, Progress progress) {
        return erode(next(dilate(f, window, mode, progress), progress), window, mode, progress);
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

    public static GrayImage stretch(GrayImage image) {
        int[] src = image.data();
        int min = 255;
        int max = 0;
        for (int v : src) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        int[] out = new int[src.length];
        if (max > min) {
            for (int i = 0; i < src.length; i++) {
                out[i] = (int) Math.round((src[i] - min) * 255.0 / (max - min));
            }
        }
        return new GrayImage(image.width(), image.height(), out);
    }
}
