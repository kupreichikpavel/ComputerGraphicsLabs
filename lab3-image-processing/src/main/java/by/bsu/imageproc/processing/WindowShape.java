package by.bsu.imageproc.processing;

public enum WindowShape {

    SQUARE("Квадрат"),
    DISK("Круг");

    private final String title;

    WindowShape(String title) {
        this.title = title;
    }

    public Window of(int radius) {
        int r = Math.max(1, radius);
        int[] dx = new int[(2 * r + 1) * (2 * r + 1)];
        int[] dy = new int[dx.length];
        int size = 0;
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                if (this == SQUARE || x * x + y * y <= (r + 0.5) * (r + 0.5)) {
                    dx[size] = x;
                    dy[size] = y;
                    size++;
                }
            }
        }
        return new Window(r, java.util.Arrays.copyOf(dx, size), java.util.Arrays.copyOf(dy, size));
    }

    @Override
    public String toString() {
        return title;
    }

    public record Window(int radius, int[] dx, int[] dy) {

        public int size() {
            return dx.length;
        }

        public int side() {
            return 2 * radius + 1;
        }
    }
}
