package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.GrayImage;

public final class Quality {

    private Quality() {
    }

    public static double mse(GrayImage reference, GrayImage image) {
        int[] a = reference.data();
        int[] b = image.data();
        if (a.length != b.length) {
            throw new IllegalArgumentException("Размеры изображений не совпадают");
        }
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }
        return sum / a.length;
    }

    public static double psnr(GrayImage reference, GrayImage image) {
        double mse = mse(reference, image);
        return mse == 0 ? Double.POSITIVE_INFINITY : 10 * Math.log10(255.0 * 255.0 / mse);
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
