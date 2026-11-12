package by.bsu.imageproc.ui;

import by.bsu.imageproc.buffer.EdgeMode;
import by.bsu.imageproc.processing.EdgeDetector;
import by.bsu.imageproc.processing.LineDetector;
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
    private final ComboBox<LineDetector.Direction> direction = new ComboBox<>();
    private final ComboBox<EdgeDetector.Operator> operator = new ComboBox<>();
    private final ComboBox<EdgeDetector.Output> output = new ComboBox<>();
    private final CheckBox darkLines = new CheckBox("Тёмные линии на светлом фоне");
    private final CheckBox cannyColor = new CheckBox("Раскрасить края по направлению");

    private final LabeledSlider radius = new LabeledSlider("Радиус окна r", 1, 60, 15,
            v -> String.format(Locale.ROOT, "%d (окно %d×%d)", (int) v, 2 * (int) v + 1, 2 * (int) v + 1), true);
    private final LabeledSlider c = new LabeledSlider("Константа C", -20, 60, 15,
            v -> String.format(Locale.ROOT, "%.0f", v), true);
    private final LabeledSlider k = new LabeledSlider("Коэффициент k", 0.05, 0.6, 0.2,
            v -> String.format(Locale.ROOT, "%.2f", v), false);
    private final LabeledSlider percent = new LabeledSlider("Порог, % от максимума отклика", 1, 100, 50,
            v -> String.format(Locale.ROOT, "%.0f%%", v), true);
    private final LabeledSlider sigma = new LabeledSlider("Предварительное сглаживание σ (0 — нет)", 0, 5, 0,
            v -> v == 0 ? "нет" : String.format(Locale.ROOT, "%.1f", v), false);
    private final LabeledSlider cannySigma = new LabeledSlider("Гаусс σ", 0.5, 5, 2,
            v -> String.format(Locale.ROOT, "%.1f", v), false);
    private final LabeledSlider high = new LabeledSlider("Верхний порог, % от max |∇f|", 5, 80, 25,
            v -> String.format(Locale.ROOT, "%.0f%%", v), true);
    private final LabeledSlider low = new LabeledSlider("Нижний порог, доля от верхнего", 0.1, 0.9, 0.4,
            v -> String.format(Locale.ROOT, "%.2f", v), false);

    private final Label edgeModeLabel = new Label("Обработка краёв изображения");
    private final Label noParams = new Label("Нет — порог вычисляется автоматически по гистограмме.");
    private final Label directionLabel = new Label("Направление линий");
    private final Label operatorLabel = new Label("Оператор");
    private final Label outputLabel = new Label("Результат");
    private final Label description = new Label();
    private final HueLegend legend = new HueLegend();
    private final Label legendLabel = new Label("Цвет = направление градиента (ось y вниз)");
    private final Label resultInfo = new Label();

    private Runnable onChange = () -> {
    };

    public ControlsPane() {
        method.getItems().addAll(Method.values());
        method.setValue(Method.OTSU);
        method.setMaxWidth(Double.MAX_VALUE);
        edgeMode.getItems().addAll(EdgeMode.values());
        edgeMode.setValue(EdgeMode.REFLECT);
        direction.getItems().addAll(LineDetector.Direction.values());
        direction.setValue(LineDetector.Direction.ALL);
        operator.getItems().addAll(EdgeDetector.Operator.values());
        operator.setValue(EdgeDetector.Operator.SOBEL);
        output.getItems().addAll(EdgeDetector.Output.values());
        output.setValue(EdgeDetector.Output.DIRECTIONS);
        for (ComboBox<?> box : new ComboBox<?>[]{edgeMode, direction, operator, output}) {
            box.setMaxWidth(Double.MAX_VALUE);
        }

        description.setWrapText(true);
        description.getStyleClass().add("description");
        resultInfo.setWrapText(true);
        resultInfo.getStyleClass().add("result-info");
        legendLabel.getStyleClass().add("muted");
        legendLabel.setWrapText(true);
        noParams.getStyleClass().add("muted");
        noParams.setWrapText(true);

        Label methodLabel = new Label("Метод");
        methodLabel.getStyleClass().add("section");
        Label paramsLabel = new Label("Параметры");
        paramsLabel.getStyleClass().add("section");
        Label howLabel = new Label("Как работает");
        howLabel.getStyleClass().add("section");
        Label resultLabel = new Label("Результат");
        resultLabel.getStyleClass().add("section");

        VBox box = new VBox(8,
                methodLabel, method,
                paramsLabel, noParams,
                edgeModeLabel, edgeMode,
                radius, c, k,
                directionLabel, direction, darkLines,
                operatorLabel, operator, outputLabel, output, sigma,
                percent,
                cannySigma, high, low, cannyColor,
                legend, legendLabel,
                resultLabel, resultInfo,
                howLabel, description);
        box.setPadding(new Insets(14));
        box.setFillWidth(true);
        setContent(box);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        getStyleClass().add("controls");
        setMinWidth(330);
        setPrefWidth(350);
        box.setMaxWidth(Double.MAX_VALUE);

        ChangeListener<Object> changed = (obs, oldValue, newValue) -> {
            updateVisibility();
            onChange.run();
        };
        method.valueProperty().addListener(changed);
        edgeMode.valueProperty().addListener(changed);
        direction.valueProperty().addListener(changed);
        operator.valueProperty().addListener(changed);
        output.valueProperty().addListener(changed);
        darkLines.selectedProperty().addListener(changed);
        cannyColor.selectedProperty().addListener(changed);
        for (LabeledSlider s : new LabeledSlider[]{radius, c, k, percent, sigma, cannySigma, high, low}) {
            s.valueProperty().addListener(changed);
        }
        updateVisibility();
    }

    public void setOnChange(Runnable action) {
        onChange = action;
    }

    public void selectMethod(Method value) {
        method.setValue(value);
    }

    public void showResultInfo(String text) {
        resultInfo.setText(text);
    }

    public Settings settings() {
        return new Settings(method.getValue(), edgeMode.getValue(), (int) radius.value(), c.value(), k.value(),
                percent.value(), direction.getValue(), darkLines.isSelected(), operator.getValue(), output.getValue(),
                sigma.value(), cannySigma.value(), high.value(), low.value(), cannyColor.isSelected());
    }

    private void updateVisibility() {
        Method m = method.getValue();
        boolean global = m == Method.ITERATIVE || m == Method.OTSU;
        boolean adaptive = m == Method.ADAPTIVE_MEAN || m == Method.ADAPTIVE_SAUVOLA;
        boolean edges = m == Method.EDGES;
        boolean canny = m == Method.CANNY;
        show(global, noParams);
        show(!global, edgeModeLabel, edgeMode);
        show(adaptive, radius);
        show(m == Method.ADAPTIVE_MEAN, c);
        show(m == Method.ADAPTIVE_SAUVOLA, k);
        show(m == Method.LINES, directionLabel, direction, darkLines);
        show(edges, operatorLabel, operator, outputLabel, output, sigma);
        show(m == Method.POINTS || m == Method.LINES || (edges && output.getValue() == EdgeDetector.Output.EDGES),
                percent);
        show(canny, cannySigma, high, low, cannyColor);
        boolean directions = (edges && output.getValue() == EdgeDetector.Output.DIRECTIONS)
                || (canny && cannyColor.isSelected());
        show(directions, legend, legendLabel);
        description.setText(m.description());
    }

    private static void show(boolean visible, Node... nodes) {
        for (Node node : nodes) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    public void setPercent(double value) {
        percent.setValue(value);
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
