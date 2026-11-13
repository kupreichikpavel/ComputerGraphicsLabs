package by.bsu.imageproc.buffer;

public enum EdgeMode {

    CLAMP("Clamp — повтор граничного пикселя"),
    REFLECT("Reflect — зеркальное отражение");

    private final String title;

    EdgeMode(String title) {
        this.title = title;
    }

    public int index(int i, int size) {
        if (i >= 0 && i < size) {
            return i;
        }
        if (size == 1) {
            return 0;
        }
        if (this == CLAMP) {
            return i < 0 ? 0 : size - 1;
        }
        int period = 2 * (size - 1);
        int m = Math.floorMod(i, period);
        return m < size ? m : period - m;
    }

    @Override
    public String toString() {
        return title;
    }
}
