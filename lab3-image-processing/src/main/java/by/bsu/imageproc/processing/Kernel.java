package by.bsu.imageproc.processing;

public record Kernel(int width, int height, int anchorX, int anchorY, double[] weights) {

    public static Kernel of3x3(double... w) {
        if (w.length != 9) {
            throw new IllegalArgumentException("Нужно 9 коэффициентов");
        }
        return new Kernel(3, 3, 1, 1, w);
    }

    public double at(int x, int y) {
        return weights[y * width + x];
    }

    public static final Kernel LAPLACIAN_POINT = of3x3(
            -1, -1, -1,
            -1, 8, -1,
            -1, -1, -1);

    public static final Kernel LINE_HORIZONTAL = of3x3(
            -1, -1, -1,
            2, 2, 2,
            -1, -1, -1);

    public static final Kernel LINE_VERTICAL = of3x3(
            -1, 2, -1,
            -1, 2, -1,
            -1, 2, -1);

    public static final Kernel LINE_PLUS_45 = of3x3(
            -1, -1, 2,
            -1, 2, -1,
            2, -1, -1);

    public static final Kernel LINE_MINUS_45 = of3x3(
            2, -1, -1,
            -1, 2, -1,
            -1, -1, 2);

    public static final Kernel ROBERTS_DIAGONAL = new Kernel(2, 2, 0, 0, new double[]{
            -1, 0,
            0, 1});

    public static final Kernel ROBERTS_ANTIDIAGONAL = new Kernel(2, 2, 0, 0, new double[]{
            0, -1,
            1, 0});

    public static final Kernel PREWITT_X = of3x3(
            -1, 0, 1,
            -1, 0, 1,
            -1, 0, 1);

    public static final Kernel PREWITT_Y = of3x3(
            -1, -1, -1,
            0, 0, 0,
            1, 1, 1);

    public static final Kernel SOBEL_X = of3x3(
            -1, 0, 1,
            -2, 0, 2,
            -1, 0, 1);

    public static final Kernel SOBEL_Y = of3x3(
            -1, -2, -1,
            0, 0, 0,
            1, 2, 1);
}
