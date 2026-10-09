package by.bsu.imageinfo.parser;

import java.util.Map;

final class Values {

    static final double INCH_IN_METERS = 0.0254;
    static final double INCH_IN_CM = 2.54;

    private Values() {
    }

    static int round(double value) {
        return (int) Math.round(value);
    }

    static void put(Map<String, String> extras, String key, String value) {
        if (value != null && !value.isBlank()) {
            extras.put(key, value.trim());
        }
    }

    static String join(String a, String b) {
        String result = ((a == null ? "" : a.trim()) + " " + (b == null ? "" : b.trim())).trim();
        return result.isEmpty() ? null : result;
    }

    static String colors(long count) {
        long mod100 = count % 100;
        long mod10 = count % 10;
        if (mod100 >= 11 && mod100 <= 14) {
            return count + " цветов";
        }
        if (mod10 == 1) {
            return count + " цвет";
        }
        if (mod10 >= 2 && mod10 <= 4) {
            return count + " цвета";
        }
        return count + " цветов";
    }
}
