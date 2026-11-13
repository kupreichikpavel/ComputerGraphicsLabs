package by.bsu.imageproc.ui;

import by.bsu.imageproc.processing.Morphology;
import by.bsu.imageproc.processing.WindowShape;

import java.util.List;

public record Sample(String file, String title, Method method, int filterRadius, WindowShape shape,
                     Morphology.Operation operation, boolean binarize, String hint) {

    private static Sample of(String file, String title, Method method, String hint) {
        return new Sample(file, title, method, 1, WindowShape.SQUARE, Morphology.Operation.TOP_HAT, false, hint);
    }

    private static Sample morphology(String file, String title, Morphology.Operation operation, int radius,
                                     boolean binarize, String hint) {
        return new Sample(file, title, Method.MORPHOLOGY, radius, WindowShape.DISK, operation, binarize, hint);
    }

    public static final List<Sample> ALL = List.of(
            of("01_bimodal_coins.png", "1. Монеты на светлом фоне", Method.OTSU,
                    "Гистограмма с двумя чёткими пиками: оба глобальных метода дают почти одинаковый порог и чистый результат."),
            of("02_otsu_better_text_two_tones.png", "2. Текст на бумаге двух оттенков — Оцу лучше", Method.OTSU,
                    "Три пика: текст, светлая и затенённая половины листа. Итеративный метод стартует со средней яркости, "
                            + "которая лежит между оттенками бумаги, и делит лист пополам. Оцу находит глобальный максимум "
                            + "σ²B — отделяет текст от бумаги."),
            of("03_iterative_better_part_with_labels.png", "3. Деталь с наклейками — итеративный лучше", Method.ITERATIVE,
                    "Задача — отделить серую деталь от тёмного стола. Итеративный метод от среднего сходится к долине "
                            + "«стол | деталь». У Оцу глобальный максимум σ²B — на разбиении «наклейки | всё остальное», "
                            + "и деталь теряется."),
            of("04_adaptive_uneven_light_text.png", "4. Текст при неравномерном освещении", Method.ADAPTIVE_SAUVOLA,
                    "Фон меняется от тёмного к светлому сильнее, чем текст отличается от фона. Любой глобальный порог "
                            + "заливает тёмную часть чёрным или теряет текст в светлой — справляется только адаптивный."),
            of("05_adaptive_shadow_document.png", "5. Документ с тенью", Method.ADAPTIVE_SAUVOLA,
                    "Резкая диагональная тень. Глобальный порог делит изображение по границе тени, адаптивный читает текст "
                            + "и в тени, и на свету."),
            of("06_salt_pepper.png", "6. Шум «соль-перец» 10% — медиана", Method.MEDIAN,
                    "Медиана 3×3 удаляет импульсы, края фигур и тонкие линии остаются резкими. Увеличьте окно до 5×5 и 7×7: "
                            + "шум уходит тот же, но тонкие линии и точки начинают пропадать."),
            of("07_salt_only.png", "7. Только белые точки — фильтр минимума", Method.MIN,
                    "Белая точка — наибольшее значение окна, минимум её всегда отбрасывает. Плата — тёмные объекты "
                            + "расширяются, светлые сужаются. Переключите на максимум: каждая белая точка раздувается в квадрат."),
            of("08_pepper_only.png", "8. Только чёрные точки — фильтр максимума", Method.MAX,
                    "Зеркальный случай: чёрная точка — наименьшее значение окна, максимум её отбрасывает. Светлые объекты "
                            + "расширяются, тёмные тонкие линии исчезают."),
            morphology("09_tophat_particles.png", "9. Светлые частицы на неоднородном фоне — Top-Hat",
                    Morphology.Operation.TOP_HAT, 6, true,
                    "Открытие кругом r = 6 убирает частицы (они меньше круга) и оставляет фон. Top-Hat = f − открытие — "
                            + "только частицы на ровном нуле, после чего Оцу находит их все. Выберите метод Оцу для исходного "
                            + "изображения: выделится светлое пятно фона вместе с частицами."),
            morphology("10_blackhat_spots.png", "10. Тёмные пятна на неоднородном фоне — Black-Hat",
                    Morphology.Operation.BLACK_HAT, 6, true,
                    "Зеркальный случай: закрытие кругом r = 6 закрашивает тёмные пятна, Black-Hat = закрытие − f оставляет "
                            + "только их. Порог Оцу на исходном изображении захватывает тёмный угол фона."),
            morphology("11_gradient_shapes.png", "11. Фигуры — морфологический градиент",
                    Morphology.Operation.GRADIENT, 1, false,
                    "Градиент = дилатация − эрозия: внутри однородной области max и min совпадают и разность равна нулю, "
                            + "на границе — перепад яркости. Получаются контуры толщиной около 2r + 1 пикселей."),
            new Sample("12_noisy_document.png", "12. Зашумлённый документ — медиана, затем Оцу", Method.MEDIAN, 1,
                    WindowShape.SQUARE, Morphology.Operation.TOP_HAT, true,
                    "Медиана 3×3, затем порог Оцу — чистый текст. Выберите метод Оцу без фильтра: каждый импульс станет "
                            + "чёрной или белой точкой результата."),
            of("13_large_4000x3000.png", "13. Большое изображение 4000×3000", Method.MEDIAN,
                    "Для проверки скорости: полосы строк обрабатываются параллельно на всех ядрах, окно не зависает. "
                            + "Поменяйте число потоков вверху и сравните время.")
    );
}
