package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.ColorImage;

public final class DirectionColors {

    private DirectionColors() {
    }

    public static int color(double angleRadians, double brightness) {
        double hue = Math.toDegrees(angleRadians);
        if (hue < 0) {
            hue += 360;
        }
        return hsv(hue, 1.0, Math.max(0, Math.min(1, brightness)));
    }

    public static ColorImage map(RealImage magnitude, RealImage angle, boolean[] mask) {
        int n = magnitude.data().length;
        double max = magnitude.max();
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            if (mask != null && !mask[i]) {
                out[i] = 0xFF000000;
                continue;
            }
            double v = max == 0 ? 0 : magnitude.data()[i] / max;
            double brightness = mask != null ? 1.0 : Math.sqrt(v);
            out[i] = color(angle.data()[i], brightness);
        }
        return new ColorImage(magnitude.width(), magnitude.height(), out);
    }

    static int hsv(double h, double s, double v) {
        double c = v * s;
        double x = c * (1 - Math.abs((h / 60) % 2 - 1));
        double m = v - c;
        double r;
        double g;
        double b;
        if (h < 60) {
            r = c; g = x; b = 0;
        } else if (h < 120) {
            r = x; g = c; b = 0;
        } else if (h < 180) {
            r = 0; g = c; b = x;
        } else if (h < 240) {
            r = 0; g = x; b = c;
        } else if (h < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        return ColorImage.rgb((int) Math.round((r + m) * 255), (int) Math.round((g + m) * 255),
                (int) Math.round((b + m) * 255));
    }
}
