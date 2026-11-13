package by.bsu.imageproc.ui;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.processing.Morphology;
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
    private final CheckBox binarize = new CheckBox("Затем бинаризовать порогом Оцу");

    private final LabeledSlider radius = new LabeledSlider("Радиус окна r", 1, 60, 15,
            v -> String.format(Locale.ROOT, "%d (окно %d×%d)", (int) v, 2 * (int) v + 1, 2 * (int) v + 1), true);
    private final LabeledSlider k = new LabeledSlider("Коэффициент k", 0.05, 0.6, 0.2,
            v -> String.format(Locale.ROOT, "%.2f", v), false);
    private final LabeledSlider filterRadius = new LabeledSlider("Радиус окна r", 1, 10, 1,
            v -> String.format(Locale.ROOT, "%d (%d×%d)", (int) v, 2 * (int) v + 1, 2 * (int) v + 1), true);

    private final Label edgeModeLabel = new Label("Обработка краёв изображения");
    private final Label noParams = new Label("Порог вычисляется автоматически по гистограмме.");
    private final Label shapeLabel = new Label("Форма окна");
    private final Label operationLabel = new Label("Операция");
    private final Label resultInfo = new Label();

    private Runnable onChange = () -> {
    };
    private boolean silent;

    public ControlsPane() {
        method.getItems().addAll(Method.values());
        method.setValue(Method.OTSU);
        edgeMode.getItems().addAll(EdgeMode.values());
        edgeMode.setValue(EdgeMode.REFLECT);
        shape.getItems().addAll(WindowShape.values());
        shape.setValue(WindowShape.SQUARE);
        operation.getItems().addAll(Morphology.Operation.values());
        operation.setValue(Morphology.Operation.TOP_HAT);
        for (ComboBox<?> box : new ComboBox<?>[]{method, edgeMode, shape, operation}) {
            box.setMaxWidth(Double.MAX_VALUE);
        }
        binarize.setWrapText(true);

        resultInfo.setWrapText(true);
        resultInfo.getStyleClass().add("result-info");
        noParams.getStyleClass().add("muted");
        noParams.setWrapText(true);

        VBox box = new VBox(8,
                section("Метод"), method,
                section("Параметры"), noParams,
                edgeModeLabel, edgeMode,
                radius, k,
                operationLabel, operation,
                shapeLabel, shape,
                filterRadius, binarize,
                section("Результат"), resultInfo);
        box.setPadding(new Insets(14));
        box.setFillWidth(true);
        box.setMaxWidth(Double.MAX_VALUE);
        setContent(box);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        getStyleClass().add("controls");
        setMinWidth(330);
        setPrefWidth(350);

        ChangeListener<Object> changed = (obs, oldValue, newValue) -> {
            updateVisibility();
            if (!silent) {
                onChange.run();
            }
        };
        for (ComboBox<?> combo : new ComboBox<?>[]{method, edgeMode, shape, operation}) {
            combo.valueProperty().addListener(changed);
        }
        binarize.selectedProperty().addListener(changed);
        for (LabeledSlider s : new LabeledSlider[]{radius, k, filterRadius}) {
            s.valueProperty().addListener(changed);
        }
        updateVisibility();
    }

    private static Label section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section");
        return label;
    }

    public void setOnChange(Runnable action) {
        onChange = action;
    }

    public void applySample(Sample sample) {
        silent = true;
        try {
            method.setValue(sample.method());
            filterRadius.setValue(sample.filterRadius());
            shape.setValue(sample.shape());
            operation.setValue(sample.operation());
            binarize.setSelected(sample.binarize());
        } finally {
            silent = false;
        }
        updateVisibility();
    }

    public void showResultInfo(String text) {
        resultInfo.setText(text);
    }

    public Settings settings() {
        return new Settings(method.getValue(), edgeMode.getValue(), (int) radius.value(), k.value(),
                shape.getValue(), (int) filterRadius.value(), operation.getValue(), binarize.isSelected());
    }

    private void updateVisibility() {
        Method m = method.getValue();
        boolean global = m == Method.ITERATIVE || m == Method.OTSU;
        boolean filter = !m.isThreshold();
        show(global, noParams);
        show(!global, edgeModeLabel, edgeMode);
        show(m == Method.ADAPTIVE_SAUVOLA, radius, k);
        show(m == Method.MORPHOLOGY, operationLabel, operation);
        show(filter, shapeLabel, shape, filterRadius, binarize);
    }

    private static void show(boolean visible, Node... nodes) {
        for (Node node : nodes) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    static final class LabeledSlider extends VBox {

        private final Slider slider;

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
            Label name = new Label(title);
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

        javafx.beans.property.DoubleProperty valueProperty() {
            return slider.valueProperty();
        }
    }
}
