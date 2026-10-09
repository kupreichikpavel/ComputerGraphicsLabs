package by.bsu.imageinfo;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

public class MainApp extends Application {

    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "gif", "tif", "tiff", "bmp", "png", "pcx");

    private final ImageInfoReader reader = new ImageInfoReader();
    private final ObservableList<ImageInfo> items = FXCollections.observableArrayList();
    private final Queue<ImageInfo> ready = new ConcurrentLinkedQueue<>();
    private final AtomicInteger done = new AtomicInteger();
    private volatile int total = -1;

    private final TableView<ImageInfo> table = new TableView<>(items);
    private final DetailsPane details = new DetailsPane();
    private final Button openButton = new Button("Открыть папку…");
    private final Button filesButton = new Button("Выбрать файлы…");
    private final Button removeButton = new Button("Убрать из списка");
    private final Spinner<Integer> threadsSpinner =
        new Spinner<>(1, 64, Runtime.getRuntime().availableProcessors() * 2);
    private final ProgressBar progress = new ProgressBar(0);
    private final Label statusLabel = new Label("Выберите папку или файлы");
    private Timeline timer;
    private long started;
    private int threads;

    @Override
    public void start(Stage stage) {
        table.getColumns().add(column("Имя файла", ImageInfo::name, 200));
        table.getColumns().add(column("Формат", ImageInfo::format, 70));
        TableColumn<ImageInfo, String> sizeColumn = column("Размер, px", ImageInfo::size, 110);
        sizeColumn.setComparator(Comparator.nullsFirst(Comparator.comparingLong(MainApp::pixels)));
        table.getColumns().add(sizeColumn);
        table.getColumns().add(column("DPI", ImageInfo::dpi, 70));
        table.getColumns().add(column("Глубина, бит", ImageInfo::bitDepth, 110));
        table.getColumns().add(column("Сжатие", ImageInfo::compression, 140));
        table.getColumns().add(column("Статус", ImageInfo::status, 125));
        table.setPlaceholder(new Label("Нет данных"));
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            details.show(selected);
            removeButton.setDisable(selected == null || timer != null);
        });
        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                removeSelected();
            }
        });
        removeButton.setDisable(true);
        removeButton.setOnAction(e -> removeSelected());

        progress.setPrefWidth(200);
        openButton.setOnAction(e -> {
            File folder = new DirectoryChooser().showDialog(stage);
            if (folder != null) {
                scan(() -> {
                    try (Stream<Path> stream = Files.walk(folder.toPath())) {
                        return stream.filter(Files::isRegularFile).filter(MainApp::isImage).toList();
                    }
                }, true);
            }
        });
        filesButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения",
                    "*.jpg", "*.jpeg", "*.gif", "*.tif", "*.tiff", "*.bmp", "*.png", "*.pcx"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*"));
            List<File> chosen = chooser.showOpenMultipleDialog(stage);
            if (chosen != null) {
                Set<Path> already = new HashSet<>();
                items.forEach(item -> already.add(item.path()));
                scan(() -> chosen.stream().map(File::toPath).filter(path -> !already.contains(path)).toList(), false);
            }
        });

        threadsSpinner.setEditable(true);
        threadsSpinner.setPrefWidth(80);
        Label threadsLabel = new Label("Потоков:");
        for (Region control : List.of(openButton, filesButton, removeButton, threadsLabel, threadsSpinner, progress)) {
            control.setMinWidth(Region.USE_PREF_SIZE);
        }
        statusLabel.setMinWidth(0);
        HBox top = new HBox(12, openButton, filesButton, removeButton, threadsLabel, threadsSpinner, progress, statusLabel);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("top-bar");
        openButton.getStyleClass().add("primary");

        SplitPane split = new SplitPane(table, details);
        split.setDividerPositions(0.68);
        BorderPane root = new BorderPane(split);
        root.setTop(top);
        stage.setTitle("Лабораторная работа 2 — информация из графических файлов");
        Scene scene = new Scene(root, 1300, 750);
        scene.getStylesheets().add(MainApp.class.getResource("app.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private void scan(Callable<List<Path>> source, boolean replace) {
        if (replace) {
            items.clear();
        }
        table.getSelectionModel().clearSelection();
        details.show(null);
        removeButton.setDisable(true);
        ready.clear();
        done.set(0);
        total = -1;
        started = System.nanoTime();
        openButton.setDisable(true);
        filesButton.setDisable(true);
        threadsSpinner.setDisable(true);
        threads = threadsSpinner.getValue();
        progress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        statusLabel.setText("Поиск файлов…");

        Thread worker = new Thread(() -> {
            List<Path> files;
            try {
                files = source.call();
            } catch (Exception e) {
                files = List.of();
            }
            ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            });
            for (Path file : files) {
                pool.execute(() -> {
                    ready.add(reader.read(file));
                    done.incrementAndGet();
                });
            }
            pool.shutdown();
            total = files.size();
        });
        worker.setDaemon(true);
        worker.start();

        timer = new Timeline(new KeyFrame(Duration.millis(100), e -> refresh()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void refresh() {
        int processed = done.get();
        List<ImageInfo> batch = new ArrayList<>();
        ImageInfo info;
        while ((info = ready.poll()) != null) {
            batch.add(info);
        }
        items.addAll(batch);

        int count = total;
        double seconds = (System.nanoTime() - started) / 1e9;
        if (count < 0) {
            statusLabel.setText("Обработано " + processed + "…");
            return;
        }
        progress.setProgress(count == 0 ? 1 : (double) processed / count);
        statusLabel.setText(String.format(Locale.ROOT, "Обработано %d из %d, %.2f с", processed, count, seconds));
        if (processed == count) {
            timer.stop();
            timer = null;
            items.sort(Comparator.comparing(ImageInfo::name, String.CASE_INSENSITIVE_ORDER));
            openButton.setDisable(false);
            filesButton.setDisable(false);
            threadsSpinner.setDisable(false);
            statusLabel.setText(String.format(Locale.ROOT, "Готово: %d файлов за %.2f с, потоков: %d, в списке: %d", count, seconds, threads, items.size()));
        }
    }

    private void removeSelected() {
        if (timer != null) {
            return;
        }
        List<ImageInfo> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }
        items.removeAll(selected);
        table.getSelectionModel().clearSelection();
        details.show(null);
        statusLabel.setText("Убрано из списка: " + selected.size() + ", осталось: " + items.size());
    }

    private static long pixels(String size) {
        String[] parts = size.split(" × ");
        return Long.parseLong(parts[0]) * Long.parseLong(parts[1]);
    }

    private static boolean isImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot >= 0 && EXTENSIONS.contains(name.substring(dot + 1));
    }

    private static <T> TableColumn<ImageInfo, T> column(String title, Function<ImageInfo, T> value, double width) {
        TableColumn<ImageInfo, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }
}