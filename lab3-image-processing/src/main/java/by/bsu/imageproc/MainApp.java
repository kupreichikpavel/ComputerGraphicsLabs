package by.bsu.imageproc;

import by.bsu.imageproc.buffer.BmpWriter;
import by.bsu.imageproc.buffer.ColorImage;
import by.bsu.imageproc.buffer.FxImages;
import by.bsu.imageproc.buffer.GrayImage;
import by.bsu.imageproc.processing.Histogram;
import by.bsu.imageproc.processing.Noise;
import by.bsu.imageproc.processing.Parallel;
import by.bsu.imageproc.processing.Progress;
import by.bsu.imageproc.processing.Quality;
import by.bsu.imageproc.processing.Result;
import by.bsu.imageproc.ui.ControlsPane;
import by.bsu.imageproc.ui.HistogramView;
import by.bsu.imageproc.ui.Processor;
import by.bsu.imageproc.ui.Sample;
import by.bsu.imageproc.ui.Settings;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainApp extends Application {

    private final ControlsPane controls = new ControlsPane();
    private final ImageView beforeView = new ImageView();
    private final ImageView afterView = new ImageView();
    private final HistogramView beforeHistogram = new HistogramView("Гистограмма до (яркость)");
    private final HistogramView afterHistogram = new HistogramView("Гистограмма после");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label status = new Label("Откройте изображение или выберите тестовое");
    private final Label hint = new Label();
    private final ComboBox<Sample> samples = new ComboBox<>();
    private final Button saveButton = new Button("Сохранить результат (BMP)…");
    private final Spinner<Integer> threads = new Spinner<>(1, 64, Runtime.getRuntime().availableProcessors());
    private final PauseTransition debounce = new PauseTransition(Duration.millis(120));
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "processing");
        thread.setDaemon(true);
        return thread;
    });

    private Stage stage;
    private GrayImage gray;
    private GrayImage reference;
    private Result result;
    private Progress running;
    private long generation;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;

        Button open = new Button("Открыть изображение…");
        open.getStyleClass().add("primary");
        open.setOnAction(e -> openFile());
        saveButton.setOnAction(e -> saveResult());
        saveButton.setDisable(true);

        samples.getItems().addAll(Sample.ALL);
        samples.setPromptText("Тестовые изображения");
        samples.setPrefWidth(330);
        samples.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Sample s) {
                return s == null ? "" : s.title();
            }

            @Override
            public Sample fromString(String s) {
                return null;
            }
        });
        samples.valueProperty().addListener((obs, o, s) -> {
            if (s != null) {
                loadSample(s);
            }
        });

        threads.setPrefWidth(76);
        threads.setEditable(true);
        threads.valueProperty().addListener((obs, o, n) -> {
            Parallel.setThreads(n);
            process();
        });
        Label threadsLabel = new Label("Потоков:");
        progressBar.setPrefWidth(180);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(10, open, samples, saveButton, spacer, threadsLabel, threads, progressBar, status);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("top-bar");
        status.setMinWidth(Region.USE_PREF_SIZE);

        hint.setWrapText(true);
        hint.getStyleClass().add("hint");
        hint.setVisible(false);
        hint.setManaged(false);

        SplitPane images = new SplitPane(imagePane("До", beforeView), imagePane("После", afterView));
        HBox histograms = new HBox(10, beforeHistogram, afterHistogram);
        HBox.setHgrow(beforeHistogram, Priority.ALWAYS);
        HBox.setHgrow(afterHistogram, Priority.ALWAYS);
        histograms.setPadding(new Insets(8, 10, 10, 10));
        VBox center = new VBox(hint, images, histograms);
        VBox.setVgrow(images, Priority.ALWAYS);

        BorderPane root = new BorderPane(center);
        root.setTop(top);
        root.setLeft(controls);

        controls.setOnChange(() -> debounce.playFromStart());
        debounce.setOnFinished(e -> process());

        Scene scene = new Scene(root, 1400, 860);
        scene.getStylesheets().add(Objects.requireNonNull(MainApp.class.getResource("app.css")).toExternalForm());
        stage.setTitle("Лабораторная работа 3 — пороговая обработка и нелинейные фильтры (вариант 10)");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();

        samples.setValue(Sample.ALL.get(0));
    }

    private StackPane imagePane(String title, ImageView view) {
        view.setPreserveRatio(true);
        view.setSmooth(true);
        Label label = new Label(title);
        label.getStyleClass().add("image-title");
        StackPane pane = new StackPane(view, label);
        StackPane.setAlignment(label, Pos.TOP_LEFT);
        pane.getStyleClass().add("image-pane");
        pane.setMinSize(200, 200);
        view.fitWidthProperty().bind(pane.widthProperty().subtract(16));
        view.fitHeightProperty().bind(pane.heightProperty().subtract(36));
        return pane;
    }

    private void openFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Изображение");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Изображения",
                "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        Image image = new Image(file.toURI().toString());
        if (image.isError()) {
            error("Не удалось открыть файл: " + file.getName());
            return;
        }
        samples.getSelectionModel().clearSelection();
        hint.setVisible(false);
        hint.setManaged(false);
        reference = null;
        setImage(image, file.getName());
    }

    private void loadSample(Sample sample) {
        try (InputStream in = MainApp.class.getResourceAsStream("samples/" + sample.file())) {
            if (in == null) {
                error("Нет тестового изображения " + sample.file());
                return;
            }
            Image image = new Image(in);
            reference = null;
            if (sample.reference() != null) {
                try (InputStream ref = MainApp.class.getResourceAsStream("samples/" + sample.reference())) {
                    if (ref != null) {
                        reference = GrayImage.fromArgb(FxImages.read(new Image(ref)));
                    }
                }
            }
            hint.setText(sample.hint());
            hint.setVisible(true);
            hint.setManaged(true);
            controls.applySample(sample);
            setImage(image, sample.file());
        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    private void setImage(Image image, String name) {
        ColorImage color = FxImages.read(image);
        gray = GrayImage.fromArgb(color);
        beforeView.setImage(image);
        beforeHistogram.show(Histogram.of(gray), null);
        if (reference != null && (reference.width() != gray.width() || reference.height() != gray.height())) {
            reference = null;
        }
        status.setText(String.format(Locale.ROOT, "%s, %d×%d", name, gray.width(), gray.height()));
        process();
    }

    private void process() {
        debounce.stop();
        if (gray == null) {
            return;
        }
        if (running != null) {
            running.cancel();
        }
        long id = ++generation;
        Settings settings = controls.settings();
        GrayImage clean = gray;
        GrayImage sampleReference = reference;
        Progress progress = new Progress(value -> Platform.runLater(() -> {
            if (id == generation) {
                progressBar.setProgress(value);
            }
        }));
        running = progress;
        progressBar.setProgress(0);
        worker.submit(() -> {
            try {
                boolean noisy = settings.noise() != Noise.Kind.NONE;
                GrayImage input = noisy ? Noise.add(clean, settings.noise(), settings.noiseAmount(), Noise.SEED) : clean;
                GrayImage ref = noisy ? clean : sampleReference;
                long started = System.nanoTime();
                Result r = Processor.run(input, settings, progress);
                double ms = (System.nanoTime() - started) / 1e6;
                String quality = "";
                if (ref != null && !settings.method().isThreshold() && !settings.binarize()) {
                    quality = String.format(Locale.ROOT, "\nPSNR относительно %s: до обработки %.2f дБ → после %.2f дБ",
                            noisy ? "изображения без шума" : "эталона без шума",
                            Quality.psnr(ref, input), Quality.psnr(ref, r.gray()));
                }
                String extra = quality;
                Platform.runLater(() -> {
                    if (id == generation) {
                        show(r, ms, input, extra);
                    }
                });
            } catch (Progress.CancelledException ignored) {
            } catch (RuntimeException | OutOfMemoryError e) {
                Platform.runLater(() -> error("Ошибка обработки: " + e));
            }
        });
    }

    private void show(Result r, double ms, GrayImage input, String extra) {
        result = r;
        beforeView.setImage(FxImages.write(input.toColor()));
        afterView.setImage(FxImages.write(r.display()));
        afterHistogram.show(Histogram.of(r.gray()), null);
        beforeHistogram.show(Histogram.of(input), r.threshold());
        controls.showResultInfo(r.info() + extra);
        progressBar.setProgress(1);
        status.setText(String.format(Locale.ROOT, "%d×%d, обработка %.0f мс, потоков: %d",
                input.width(), input.height(), ms, Parallel.threads()));
        saveButton.setDisable(false);
    }

    private void saveResult() {
        if (result == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить результат");
        chooser.setInitialFileName("result.bmp");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BMP", "*.bmp"));
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            BmpWriter.write(result.display(), file.toPath());
        } catch (IOException e) {
            error("Не удалось сохранить: " + e.getMessage());
        }
    }

    private void error(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}
