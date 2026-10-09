package by.bsu.imageinfo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DetailsPane extends ScrollPane {

    private static final Set<String> PREVIEW_FORMATS = Set.of("JPEG", "PNG", "GIF", "BMP");
    private static final Map<String, String> EXTRA_HINTS = Map.ofEntries(
            Map.entry("Размер палитры", "Число цветов в таблице цветов. При глубине до 8 бит пиксель хранит не цвет, "
                    + "а номер записи в палитре, где лежат значения R, G, B."),
            Map.entry("Цветовые компоненты", "Число компонент из маркера SOF. Цветной JPEG хранит яркость Y и две "
                    + "цветоразностные компоненты Cb и Cr, а не R, G, B."),
            Map.entry("Субдискретизация цветности", "Во сколько раз Cb и Cr хранятся с меньшим разрешением, чем яркость Y "
                    + "(глаз менее чувствителен к цвету). 4:4:4 — без прореживания, 4:2:0 — вдвое по обеим осям. "
                    + "Считается по коэффициентам дискретизации компонент в SOF."),
            Map.entry("Камера", "Производитель и модель устройства из Exif (теги 271 и 272)."),
            Map.entry("Дата", "Дата и время из Exif (тег 306 DateTime)."),
            Map.entry("Тип цвета", "Байт Color type чанка IHDR. Задаёт число каналов: 0 и 3 — один, 4 — два, 2 — три, 6 — четыре."),
            Map.entry("Чересстрочность", "Adam7 — строки хранятся в 7 проходов, и при загрузке картинка сначала "
                    + "появляется грубо, затем уточняется."),
            Map.entry("Версия GIF", "Сигнатура GIF87a или GIF89a. В 89a добавлены прозрачность и анимация."),
            Map.entry("Кадров", "Число блоков изображения в файле. Больше одного — анимация."),
            Map.entry("Порядок строк", "Положительная высота в заголовке BMP — строки хранятся снизу вверх, "
                    + "отрицательная — сверху вниз."),
            Map.entry("Каналов", "Тег 277 SamplesPerPixel: 1 — серый или палитра, 3 — RGB, 4 — RGBA или CMYK."),
            Map.entry("Цветовая модель", "Тег 262 PhotometricInterpretation: как трактовать значения пикселей."),
            Map.entry("Цветовых плоскостей", "24-битный PCX хранит R, G и B тремя отдельными плоскостями по 8 бит."),
            Map.entry("Версия PCX", "Байт 1 заголовка, версия PC Paintbrush. Версия 5 поддерживает 24 бита и 256 цветов."));

    private final VBox content = new VBox(14);

    public DetailsPane() {
        getStyleClass().add("details");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        content.setPadding(new Insets(16));
        setContent(content);
        show(null);
    }

    public void show(ImageInfo info) {
        content.getChildren().clear();
        if (info == null) {
            Label hint = new Label("Выберите файл в таблице, чтобы увидеть его характеристики.");
            hint.setWrapText(true);
            hint.getStyleClass().add("muted");
            content.getChildren().add(hint);
            return;
        }

        Label title = new Label(info.name());
        title.getStyleClass().add("details-title");
        title.setWrapText(true);
        Label path = new Label(info.path().toAbsolutePath().toString());
        path.getStyleClass().add("muted");
        path.setWrapText(true);

        content.getChildren().addAll(new VBox(2, title, path), preview(info),
                row("Статус", info.status(), "OK — заголовок прочитан; «Повреждён» — заголовок прочитать не удалось; "
                        + "«Не изображение» — сигнатура файла не относится к поддерживаемым форматам."),
                row("Формат", text(info.format()), "Определяется по сигнатуре в первых байтах файла, а не по расширению: "
                        + "BMP — «BM», PNG — 89 50 4E 47, JPEG — FF D8 FF, GIF — «GIF8», TIFF — «II» или «MM», PCX — 0A."),
                row("Размер изображения", info.size() == null ? "—" : info.size() + " пикселей",
                        "Ширина × высота. Хранится в заголовке: BITMAPINFOHEADER (BMP), чанк IHDR (PNG), маркер SOF (JPEG), "
                                + "Logical Screen Descriptor (GIF), теги 256/257 (TIFF), Xmax − Xmin + 1 (PCX)."),
                row("Разрешение", info.dpi() == null ? "не указано" : info.dpi() + " dpi",
                        "Точек на дюйм при печати, на экранный вид не влияет. BMP и PNG хранят пиксели на метр (× 0.0254), "
                                + "JPEG — в JFIF или Exif, TIFF — теги 282/296, PCX — HDpi. GIF разрешение не хранит."),
                row("Глубина цвета", info.bitDepth() == null ? "—" : info.bitDepth() + " бит",
                        "Бит на пиксель, задаёт число цветов 2^n. JPEG: 8 × число компонент; PNG: бит на канал × число каналов; "
                                + "GIF: log₂ размера палитры; PCX: бит × число плоскостей."),
                row("Сжатие", text(info.compression()),
                        "Алгоритм упаковки пикселей. RLE, LZW, Deflate, PackBits — без потерь, DCT в JPEG — с потерями."),
                row("Размер файла", fileSize(info), "Сколько файл занимает на диске."));

        if (!info.extras().isEmpty()) {
            Label section = new Label("Дополнительно");
            section.getStyleClass().add("section-title");
            content.getChildren().add(section);
            info.extras().forEach((name, value) ->
                    content.getChildren().add(row(name, value, EXTRA_HINTS.getOrDefault(name, ""))));
        }
    }

    private StackPane preview(ImageInfo info) {
        StackPane box = new StackPane();
        box.getStyleClass().add("preview");
        if (!"OK".equals(info.status()) || !PREVIEW_FORMATS.contains(info.format())) {
            Label note = new Label(info.format() == null ? "Предпросмотр недоступен"
                    : "JavaFX не умеет показывать " + info.format() + ", характеристики прочитаны из заголовка");
            note.setWrapText(true);
            note.getStyleClass().add("muted");
            box.getChildren().add(note);
            box.setMinHeight(70);
            return box;
        }
        ImageView view = new ImageView(new Image(info.path().toUri().toString(), 360, 240, true, true, true));
        view.setPreserveRatio(true);
        box.getChildren().add(view);
        box.setMinHeight(256);
        return box;
    }

    private static VBox row(String name, String value, String explanation) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("prop-name");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("prop-value");
        valueLabel.setWrapText(true);
        Label hint = new Label(explanation);
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        VBox row = new VBox(3, nameLabel, valueLabel, hint);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private static String fileSize(ImageInfo info) {
        try {
            long bytes = Files.size(info.path());
            if (bytes < 1024) {
                return bytes + " Б";
            }
            if (bytes < 1024 * 1024) {
                return String.format(Locale.ROOT, "%.1f КБ", bytes / 1024.0);
            }
            return String.format(Locale.ROOT, "%.1f МБ", bytes / 1024.0 / 1024.0);
        } catch (Exception e) {
            return "—";
        }
    }

    private static String text(String value) {
        return value == null ? "—" : value;
    }
}