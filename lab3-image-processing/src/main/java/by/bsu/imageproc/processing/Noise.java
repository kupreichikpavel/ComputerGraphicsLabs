package by.bsu.imageproc.processing;

import by.bsu.imageproc.buffer.GrayImage;

import java.util.SplittableRandom;

public final class Noise {

    public enum Kind {
        NONE("Нет"),
        SALT_PEPPER("Импульсный «соль-перец»"),
        SALT("Только «соль» (белые точки)"),
        PEPPER("Только «перец» (чёрные точки)"),
        GAUSSIAN("Гауссов"),
        UNIFORM("Равномерный"),
        MIXED("Гауссов + импульсный");

        private final String title;

        Kind(String title) {
            this.title = title;
        }

        public boolean impulsive() {
            return this == SALT_PEPPER || this == SALT || this == PEPPER;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public static final long SEED = 20261113L;
    public static final double MIXED_SIGMA = 12;

    private Noise() {
    }

    public static GrayImage add(GrayImage image, Kind kind, double amount, long seed) {
        int[] src = image.data();
        int[] out = src.clone();
        SplittableRandom random = new SplittableRandom(seed);
        switch (kind) {
            case NONE -> {
            }
            case SALT_PEPPER -> impulses(out, amount / 100.0, random, 0.5);
            case SALT -> impulses(out, amount / 100.0, random, 1.0);
            case PEPPER -> impulses(out, amount / 100.0, random, 0.0);
            case GAUSSIAN -> {
                for (int i = 0; i < out.length; i++) {
                    out[i] = clamp(out[i] + gaussian(random) * amount);
                }
            }
            case UNIFORM -> {
                double half = amount * Math.sqrt(3);
                for (int i = 0; i < out.length; i++) {
                    out[i] = clamp(out[i] + (random.nextDouble() * 2 - 1) * half);
                }
            }
            case MIXED -> {
                for (int i = 0; i < out.length; i++) {
                    out[i] = clamp(out[i] + gaussian(random) * MIXED_SIGMA);
                }
                impulses(out, amount / 100.0, random, 0.5);
            }
        }
        return new GrayImage(image.width(), image.height(), out);
    }

    private static void impulses(int[] data, double density, SplittableRandom random, double saltShare) {
        for (int i = 0; i < data.length; i++) {
            if (random.nextDouble() < density) {
                data[i] = random.nextDouble() < saltShare ? 255 : 0;
            }
        }
    }

    private static double gaussian(SplittableRandom random) {
        double u1 = 1 - random.nextDouble();
        double u2 = random.nextDouble();
        return Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
    }

    private static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }
}
