package by.bsu.imageproc.ui;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.processing.LinearFilter;
import by.bsu.imageproc.processing.Morphology;
import by.bsu.imageproc.processing.Noise;
import by.bsu.imageproc.processing.WindowShape;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.function.DoubleFunction;

public final class ControlsPane extends ScrollPane {

    private final ComboBox<Method> method = new ComboBox<>();
    private final ComboBox<EdgeMode> edgeMode = new ComboBox<>();
    private final ComboBox<WindowShape> shape = new ComboBox<>();
    private final ComboBox<Morphology.Operation> operation = new ComboBox<>();
    private final ComboBox<LinearFilter.Kind> linearKind = new ComboBox<>();
    private final ComboBox<Noise.Kind> noise = new ComboBox<>();
    private final CheckBox naive = new CheckBox("Наивный алгоритм (сортировка окна) — для сравнения скорости");
    private final CheckBox stretch = new CheckBox("Растянуть результат на 0…255");
    private final CheckBox binarize = new CheckBox("Затем бинаризовать порогом Оцу");

    private final LabeledSlider radius = new LabeledSlider("Радиус окна r", 1, 60, 15,
            v -> String.format(Locale.ROOT, "%d (окно %d×%d)", (int) v, 2 * (int) v + 1, 2 * (int) v + 1), true);
    private final LabeledSlider c = new LabeledSlider("Константа C", -20, 60, 15,
            v -> String.format(Locale.ROOT, "%.0f", v), true);
    private final LabeledSlider k = new LabeledSlider("Коэффициент k", 0.05, 0.6, 0.2,
            v -> String.format(Locale.ROOT, "%.2f", v), false);
    private final LabeledSlider prefilter = new LabeledSlider("Предварительный медианный фильтр", 0, 5, 0,
            v -> v == 0 ? "нет" : String.format(Locale.ROOT, "%d×%d", 2 * (int) v + 1, 2 * (int) v + 1), true);
    private final LabeledSlider filterRadius = new LabeledSlider("Радиус окна r", 1, 15, 1,
            v -> String.format(Locale.ROOT, "%d (%d×%d)", (int) v, 2 * (int) v + 1, 2 * (int) v + 1), true);
    private final LabeledSlider trim = new LabeledSlider("Отбросить с каждой стороны ряда", 0, 45, 25,
            v -> String.format(Locale.ROOT, "%.0f%%", v), true);
    private final LabeledSlider percentile = new LabeledSlider("Процентиль p", 0, 100, 50,
            v -> String.format(Locale.ROOT, "%.0f%%", v), true);
    private final LabeledSlider maxRadius = new LabeledSlider("Максимальный размер окна Smax", 1, 10, 3,
            v -> String.format(Locale.ROOT, "%d×%d", 2 * (int) v + 1, 2 * (int) v + 1), true);
    private final LabeledSlider sigma = new LabeledSlider("σ Гаусса", 0.5, 5, 1,
            v -> String.format(Locale.ROOT, "%.1f", v), false);
    private final LabeledSlider noiseAmount = new LabeledSlider("Сила шума", 0, 60, 20,
            v -> String.format(Locale.ROOT, "%.0f", v), true);

    private final Label edgeModeLabel = new Label("Обработка краёв изображения");
    private final Label noParams = new Label("Порог вычисляется автоматически по гистограмме.");
    private final Label shapeLabel = new Label("Форма окна (структурирующий элемент)");
    private final Label operationLabel = new Label("Операция");
    private final Label linearLabel = new Label("Фильтр");
    private final Label description = new Label();
    private final Label resultInfo = new Label();

    private Runnable onChange = () -> {
    };
    private boolean silent;

    public ControlsPane() {
        method.getItems().addAll(Method.values());
        method.setValue(Method.MEDIAN);
        edgeMode.getItems().addAll(EdgeMode.values());
        edgeMode.setValue(EdgeMode.REFLECT);
        shape.getItems().addAll(WindowShape.values());
        shape.setValue(WindowShape.SQUARE);
        operation.getItems().addAll(Morphology.Operation.values());
        operation.setValue(Morphology.Operation.TOP_HAT);
        linearKind.getItems().addAll(LinearFilter.Kind.values());
        linearKind.setValue(LinearFilter.Kind.BOX);
        noise.getItems().addAll(Noise.Kind.values());
        noise.setValue(Noise.Kind.NONE);
        for (ComboBox<?> box : new ComboBox<?>[]{method, edgeMode, shape, operation, linearKind, noise}) {
            box.setMaxWidth(Double.MAX_VALUE);
        }
        for (CheckBox box : new CheckBox[]{naive, stretch, binarize}) {
            box.setWrapText(true);
        }
        stretch.setSelected(true);

        description.setWrapText(true);
        description.getStyleClass().add("description");
        resultInfo.setWrapText(true);
        resultInfo.getStyleClass().add("result-info");
        noParams.getStyleClass().add("muted");
        noParams.setWrapText(true);

        VBox box = new VBox(8,
                section("Метод"), method,
                section("Параметры"), noParams,
                edgeModeLabel, edgeMode,
                radius, c, k, prefilter,
                linearLabel, linearKind,
                operationLabel, operation,
                shapeLabel, shape,
                filterRadius, trim, percentile, maxRadius, sigma,
                naive, stretch, binarize,
                section("Шум (добавляется к исходному)"), noise, noiseAmount,
                section("Результат"), resultInfo,
                section("Как работает"), description);
        box.setPadding(new Insets(14));
        box.setFillWidth(true);
        box.setMaxWidth(Double.MAX_VALUE);
        setContent(box);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        getStyleClass().add("controls");
        setMinWidth(330);
        setPrefWidth(360);

        ChangeListener<Object> changed = (obs, oldValue, newValue) -> {
            updateVisibility();
            if (!silent) {
                onChange.run();
            }
        };
        for (ComboBox<?> combo : new ComboBox<?>[]{method, edgeMode, shape, operation, linearKind, noise}) {
            combo.valueProperty().addListener(changed);
        }
        for (CheckBox check : new CheckBox[]{naive, stretch, binarize}) {
            check.selectedProperty().addListener(changed);
        }
        for (LabeledSlider s : new LabeledSlider[]{radius, c, k, prefilter, filterRadius, trim, percentile, maxRadius,
                sigma, noiseAmount}) {
            s.valueProperty().addListener(changed);
        }
        updateVisibility();
    }

    private static Label section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section");
        label.setWrapText(true);
        return label;
    }

    public void setOnChange(Runnable action) {
        onChange = action;
    }

    public void applySample(Sample sample) {
        silent = true;
        try {
            method.setValue(sample.method());
            Sample.Preset p = sample.preset();
            prefilter.setValue(p.prefilter());
            filterRadius.setValue(p.filterRadius());
            maxRadius.setValue(p.maxRadius());
            trim.setValue(p.trim());
            shape.setValue(p.shape());
            operation.setValue(p.operation());
            binarize.setSelected(p.binarize());
            linearKind.setValue(p.linearKind());
            naive.setSelected(false);
            stretch.setSelected(true);
            noise.setValue(Noise.Kind.NONE);
        } finally {
            silent = false;
        }
        updateVisibility();
    }

    public void showResultInfo(String text) {
        resultInfo.setText(text);
    }

    public Settings settings() {
        return new Settings(method.getValue(), edgeMode.getValue(), (int) radius.value(), c.value(), k.value(),
                (int) prefilter.value(), shape.getValue(), (int) filterRadius.value(), trim.value() / 100.0,
                percentile.value(), (int) maxRadius.value(), operation.getValue(), stretch.isSelected(),
                linearKind.getValue(), sigma.value(), naive.isSelected(), binarize.isSelected(), noise.getValue(),
                noiseAmount.value());
    }

    private void updateVisibility() {
        Method m = method.getValue();
        boolean global = m == Method.ITERATIVE || m == Method.OTSU;
        boolean adaptive = m == Method.ADAPTIVE_MEAN || m == Method.ADAPTIVE_SAUVOLA;
        boolean linear = m == Method.LINEAR;
        boolean box = linear && linearKind.getValue() == LinearFilter.Kind.BOX;
        boolean windowed = m.isRankFilter() || m == Method.MORPHOLOGY;
        show(global, noParams);
        show(!global || prefilter.value() > 0, edgeModeLabel, edgeMode);
        show(adaptive, radius);
        show(m == Method.ADAPTIVE_MEAN, c);
        show(m == Method.ADAPTIVE_SAUVOLA, k);
        show(m.isThreshold(), prefilter);
        show(linear, linearLabel, linearKind);
        show(linear && !box, sigma);
        show(m == Method.MORPHOLOGY, operationLabel, operation);
        show(m == Method.MORPHOLOGY && operation.getValue().isDifference(), stretch);
        show(windowed, shapeLabel, shape);
        show(windowed || box, filterRadius);
        show(m == Method.ALPHA_TRIMMED, trim);
        show(m == Method.PERCENTILE, percentile);
        show(m == Method.ADAPTIVE_MEDIAN, maxRadius);
        show(m.isRankFilter(), naive);
        show(!m.isThreshold(), binarize);
        Noise.Kind kind = noise.getValue();
        show(kind != Noise.Kind.NONE, noiseAmount);
        noiseAmount.setTitle(switch (kind) {
            case GAUSSIAN, UNIFORM -> "СКО шума σ, уровней яркости";
            case MIXED -> "Доля импульсов, % (плюс гауссов σ = 12)";
            default -> "Доля повреждённых пикселей, %";
        });
        description.setText(m.description());
    }

    private static void show(boolean visible, Node... nodes) {
        for (Node node : nodes) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    static final class LabeledSlider extends VBox {

        private final Slider slider;
        private final Label name;

        LabeledSlider(String title, double min, double max, double initial, DoubleFunction<String> format,
                      boolean integer) {
            super(2);
            slider = new Slider(min, max, initial);
            if (integer) {
                slider.setMajorTickUnit(1);
                slider.setMinorTickCount(0);
                slider.setSnapToTicks(true);
                slider.setBlockIncrement(1);
            }
            name = new Label(title);
            name.setWrapText(true);
            name.setMinWidth(0);
            Label value = new Label(format.apply(initial));
            value.setMinWidth(Region.USE_PREF_SIZE);
            value.getStyleClass().add("slider-value");
            slider.valueProperty().addListener((obs, o, n) -> value.setText(format.apply(n.doubleValue())));
            HBox head = new HBox(8, name, value);
            HBox.setHgrow(name, Priority.ALWAYS);
            name.setMaxWidth(Double.MAX_VALUE);
            getChildren().addAll(head, slider);
        }

        double value() {
            return slider.getValue();
        }

        void setValue(double v) {
            slider.setValue(v);
        }

        void setTitle(String title) {
            name.setText(title);
        }

        javafx.beans.property.DoubleProperty valueProperty() {
            return slider.valueProperty();
        }
    }
}
