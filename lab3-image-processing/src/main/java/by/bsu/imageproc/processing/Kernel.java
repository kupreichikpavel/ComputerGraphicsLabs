package by.bsu.imageproc.processing;

public record Kernel(int width, int height, int anchorX, int anchorY, double[] weights) {

    public static Kernel outer(double[] k1d) {
        int n = k1d.length;
        double[] w = new double[n * n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                w[y * n + x] = k1d[y] * k1d[x];
            }
        }
        return new Kernel(n, n, n / 2, n / 2, w);
    }

    public double at(int x, int y) {
        return weights[y * width + x];
    }
}
