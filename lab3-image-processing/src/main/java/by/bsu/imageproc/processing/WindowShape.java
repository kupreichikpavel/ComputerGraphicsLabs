package by.bsu.imageproc.processing;

public enum WindowShape {

    SQUARE("Квадрат"),
    CROSS("Крест"),
    DISK("Круг");

    private final String title;

    WindowShape(String title) {
        this.title = title;
    }

    public Window of(int radius) {
        int r = Math.max(0, radius);
        int[] from = new int[2 * r + 1];
        int[] to = new int[2 * r + 1];
        int count = 0;
        for (int dy = -r; dy <= r; dy++) {
            int half = switch (this) {
                case SQUARE -> r;
                case CROSS -> dy == 0 ? r : 0;
                case DISK -> (int) Math.floor(Math.sqrt((r + 0.5) * (r + 0.5) - dy * dy));
            };
            from[dy + r] = -half;
            to[dy + r] = half;
            count += 2 * half + 1;
        }
        return new Window(r, from, to, count);
    }

    @Override
    public String toString() {
        return title;
    }

    public record Window(int radius, int[] from, int[] to, int size) {

        public boolean contains(int dx, int dy) {
            return Math.abs(dy) <= radius && dx >= from[dy + radius] && dx <= to[dy + radius];
        }

        public int side() {
            return 2 * radius + 1;
        }
    }
}
