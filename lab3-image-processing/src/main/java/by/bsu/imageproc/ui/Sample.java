package by.bsu.imageproc.ui;

import by.bsu.imageproc.processing.LinearFilter;
import by.bsu.imageproc.processing.Morphology;
import by.bsu.imageproc.processing.WindowShape;

import java.util.List;

public record Sample(String file, String reference, String title, Method method, Preset preset, String hint) {

    public record Preset(int prefilter, int filterRadius, int maxRadius, double trim, WindowShape shape,
                         Morphology.Operation operation, boolean binarize, LinearFilter.Kind linearKind) {

        static final Preset DEFAULT = new Preset(0, 1, 3, 25, WindowShape.SQUARE, Morphology.Operation.TOP_HAT,
                false, LinearFilter.Kind.BOX);

        Preset prefilter(int value) {
            return new Preset(value, filterRadius, maxRadius, trim, shape, operation, binarize, linearKind);
        }

        Preset radius(int value) {
            return new Preset(prefilter, value, maxRadius, trim, shape, operation, binarize, linearKind);
        }

        Preset morphology(WindowShape s, int r, Morphology.Operation op, boolean otsu) {
            return new Preset(prefilter, r, maxRadius, trim, s, op, otsu, linearKind);
        }
    }

    private static Sample of(String file, String title, Method method, String hint) {
        return new Sample(file, null, title, method, Preset.DEFAULT, hint);
    }

    private static Sample noisy(String name, String title, Method method, Preset preset, String hint) {
        return new Sample(name + ".png", "ref/" + name + "_ref.png", title, method, preset, hint);
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
            of("05_adaptive_shadow_document.png", "5. Документ с тенью", Method.ADAPTIVE_MEAN,
                    "Резкая диагональная тень. Глобальный порог делит изображение по границе тени, адаптивный читает текст "
                            + "и в тени, и на свету."),
            noisy("06_salt_pepper_10", "6. Шум «соль-перец» 10% — медиана", Method.MEDIAN, Preset.DEFAULT,
                    "Медиана 3×3 удаляет почти все импульсы, края фигур и буквы остаются резкими: PSNR 15 → 29 дБ. "
                            + "Переключите на «Линейное сглаживание»: импульсы размазываются в серые пятна, края размываются."),
            noisy("07_salt_pepper_40", "7. Шум «соль-перец» 40% — адаптивная медиана", Method.ADAPTIVE_MEDIAN,
                    Preset.DEFAULT,
                    "При 40% импульсов в окне 3×3 они часто составляют большинство и медиана сама оказывается импульсом "
                            + "(18 дБ). Медиана 5×5 и 7×7 шум убирает, но съедает тонкие линии и точки (24 и 23 дБ). "
                            + "Адаптивная медиана расширяет окно только там, где нужно, и не трогает неповреждённые пиксели: 27 дБ."),
            noisy("08_salt_only", "8. Только белые импульсы — фильтр минимума", Method.MIN, Preset.DEFAULT,
                    "Белая точка — максимальный элемент ряда, минимум её всегда отбрасывает: «соли» не остаётся совсем. "
                            + "Плата — тёмные объекты расширяются, светлые сужаются, поэтому по PSNR минимум (22 дБ) уступает "
                            + "медиане (29 дБ). Максимум здесь раздувает каждую белую точку в квадрат (8 дБ)."),
            noisy("09_pepper_only", "9. Только чёрные импульсы — фильтр максимума", Method.MAX, Preset.DEFAULT,
                    "Зеркальный случай: чёрная точка — минимальный элемент ряда, максимум её отбрасывает. Светлые объекты "
                            + "расширяются, тёмные тонкие линии исчезают (23 дБ против 29 у медианы)."),
            noisy("10_uniform_noise", "10. Равномерный шум — средняя точка", Method.MIDPOINT, Preset.DEFAULT,
                    "У равномерного шума нет выбросов: отклонения ограничены ±43. Тогда min и max окна лежат близко к краям "
                            + "интервала, и (min + max)/2 оценивает яркость точнее всего. Окно 3×3: средняя точка 30 дБ, "
                            + "усреднение 29, медиана 26."),
            noisy("11_mixed_noise", "11. Гауссов + импульсный — усечённое среднее", Method.ALPHA_TRIMMED,
                    Preset.DEFAULT.radius(2),
                    "Сильный гауссов шум (σ = 30) и 5% импульсов. Отбрасываются по 25% наименьших и наибольших значений "
                            + "окна 5×5 — туда попадают импульсы, — остальные усредняются и гасят гауссову составляющую. "
                            + "Окно 5×5: усечённое среднее 29,9 дБ, медиана 29,3, усреднение 28,1, средняя точка 16,5."),
            new Sample("12_tophat_particles.png", null, "12. Частицы на неоднородном фоне — Top-Hat", Method.MORPHOLOGY,
                    Preset.DEFAULT.morphology(WindowShape.DISK, 6, Morphology.Operation.TOP_HAT, true),
                    "Открытие кругом r = 6 убирает частицы (они меньше элемента) и оставляет фон. Top-Hat = f − открытие — "
                            + "только частицы на ровном нуле, после чего Оцу находит их все. Попробуйте Оцу на исходном "
                            + "изображении: яркое пятно фона выделится вместе с частицами."),
            noisy("13_noisy_document", "13. Зашумлённый документ — медиана перед порогом", Method.OTSU,
                    Preset.DEFAULT.prefilter(1),
                    "Порог Оцу после медианы 3×3 даёт чистый текст. Поставьте «Предварительный медианный фильтр» в «нет» — "
                            + "каждый импульс станет чёрной или белой точкой результата."),
            noisy("14_large_4000x3000", "14. Большое изображение 4000×3000", Method.MEDIAN, Preset.DEFAULT.radius(2),
                    "Для проверки скорости: алгоритм Хуанга со скользящей гистограммой, полосы строк обрабатываются "
                            + "параллельно. Включите «Наивный алгоритм» и сравните время; поменяйте число потоков.")
    );
}
