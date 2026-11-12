package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.ColorImage;
import by.bsu.imageproc.buffer.GrayImage;

public record Result(GrayImage gray, ColorImage color, Integer threshold, String info) {

    public static Result gray(GrayImage gray, Integer threshold, String info) {
        return new Result(gray, null, threshold, info);
    }

    public static Result color(ColorImage color, GrayImage grayForHistogram, String info) {
        return new Result(grayForHistogram, color, null, info);
    }

    public ColorImage display() {
        return color != null ? color : gray.toColor();
    }
}
