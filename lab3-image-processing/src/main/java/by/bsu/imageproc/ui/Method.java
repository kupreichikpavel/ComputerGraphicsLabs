package by.bsu.imageproc.ui;

public enum Method {

    ITERATIVE("Глобальный порог — итеративный"),
    OTSU("Глобальный порог — метод Оцу"),
    ADAPTIVE_SAUVOLA("Адаптивный порог — Саувола"),
    MEDIAN("Медианный фильтр"),
    MIN("Фильтр минимума"),
    MAX("Фильтр максимума"),
    MORPHOLOGY("Морфология (на основе min и max)");

    private final String title;

    Method(String title) {
        this.title = title;
    }

    public boolean isThreshold() {
        return this == ITERATIVE || this == OTSU || this == ADAPTIVE_SAUVOLA;
    }

    @Override
    public String toString() {
        return title;
    }
}
