import sys
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFont

out = Path(sys.argv[1]); out.mkdir(parents=True, exist_ok=True)
rng = np.random.default_rng(2026)
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf"
TEXT = ("Пороговая обработка изображений. Глобальный порог выбирается по гистограмме "
        "одним значением T для всего кадра. Адаптивный порог вычисляется для каждого "
        "пикселя по его окрестности, поэтому неравномерное освещение не мешает. "
        "Метод Оцу максимизирует межклассовую дисперсию, итеративный метод "
        "уточняет порог как среднее двух средних. Сегментация выделяет точки, "
        "линии и перепады яркости с помощью масок свёртки.")

def save(name, arr):
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "L").convert("RGB").save(out / name, optimize=True)

def text_mask(w, h, size, margin=30, text=TEXT, repeat=3):
    img = Image.new("L", (w, h), 0); d = ImageDraw.Draw(img); f = ImageFont.truetype(FONT, size)
    words = (text + " ") * repeat
    words = words.split(); line = ""; y = margin
    for word in words:
        trial = (line + " " + word).strip()
        if d.textlength(trial, font=f) > w - 2 * margin:
            d.text((margin, y), line, fill=255, font=f); y += int(size * 1.35); line = word
            if y > h - margin - size: break
        else:
            line = trial
    return np.array(img) > 127

# 1. обычный двухмодальный случай — оба глобальных метода работают
h, w = 360, 520
img = np.full((h, w), 205.0)
yy, xx = np.mgrid[0:h, 0:w]
gt = np.zeros((h, w), bool)
for cx, cy, r in [(110, 110, 60), (270, 120, 75), (420, 100, 50), (150, 260, 70), (330, 270, 60), (450, 260, 45)]:
    gt |= (xx - cx) ** 2 + (yy - cy) ** 2 < r * r
img[gt] = 70
img += rng.normal(0, 12, (h, w))
save("01_bimodal_coins.png", img)

# 2. Оцу лучше: тёмный текст на листе, половина листа в тени (два оттенка бумаги)
h, w = 420, 640
paper = np.full((h, w), 245.0); paper[:, w // 2:] = 190.0
mask = text_mask(w, h, 22)
img = np.where(mask, 45.0, paper) + rng.normal(0, 5, (h, w))
save("02_otsu_better_text_two_tones.png", img)

# 3. Итеративный лучше: серая деталь на тёмном столе с яркими наклейками
h, w = 400, 600
img = np.full((h, w), 14.0)
part = np.zeros((h, w), bool); part[40:360, 50:550] = True
img[part] = 92.0
labels = np.zeros((h, w), bool)
for (x0, y0, x1, y1) in [(80, 70, 200, 150), (380, 90, 520, 170), (100, 260, 230, 330), (400, 250, 510, 330)]:
    labels[y0:y1, x0:x1] = True
img[labels] = 194.0
img += np.where(part, rng.normal(0, 8, (h, w)), rng.normal(0, 10, (h, w)))
save("03_iterative_better_part_with_labels.png", img)

# 4. Адаптивный: текст при сильно неравномерном освещении (градиент + виньетка)
h, w = 480, 700
yy, xx = np.mgrid[0:h, 0:w]
light = 70 + 175 * (xx / w) ** 0.8
light *= 1 - 0.45 * (((xx - w * 0.75) / w) ** 2 + ((yy - h * 0.4) / h) ** 2)
mask = text_mask(w, h, 20)
img = np.where(mask, light * 0.42, light) + rng.normal(0, 4, (h, w))
save("04_adaptive_uneven_light_text.png", img)

# 5. Адаптивный: документ с тенью от руки/предмета (резкая тёмная полоса)
h, w = 480, 700
yy, xx = np.mgrid[0:h, 0:w]
light = np.full((h, w), 225.0)
shadow = 1 / (1 + np.exp(-(xx - 0.55 * yy - 260) / 18))
light = light * (0.38 + 0.62 * shadow)
mask = text_mask(w, h, 19)
img = np.where(mask, light * 0.45, light) + rng.normal(0, 4, (h, w))
save("05_adaptive_shadow_document.png", img)

# 6. Точки: одиночные яркие пиксели на фоне с плавным градиентом и крупными пятнами
h, w = 400, 600
yy, xx = np.mgrid[0:h, 0:w]
img = 60 + 60 * xx / w + 20 * np.sin(yy / 40)
for cx, cy, r in [(150, 120, 45), (420, 260, 60), (300, 330, 30)]:
    img[(xx - cx) ** 2 + (yy - cy) ** 2 < r * r] += 90
pts = rng.integers([20, 20], [w - 20, h - 20], size=(14, 2))
for x, y in pts:
    img[y, x] += 120
img += rng.normal(0, 3, (h, w))
save("06_points_isolated_pixels.png", img)

# 7. Линии: тонкие линии четырёх направлений на тёмном фоне
h, w = 400, 600
img = np.full((h, w), 40.0)
img[80, 40:560] = 200; img[300, 60:300] = 200
img[30:370, 120] = 200; img[50:250, 480] = 200
for t in range(0, 220): img[340 - t, 200 + t] = 200
for t in range(0, 180): img[60 + t, 300 + t] = 200
img[180:200, 380:440] = 200
img += rng.normal(0, 6, (h, w))
save("07_lines_four_directions.png", img)

# 8. Тёмные линии на светлом фоне (чертёж/провода)
h, w = 400, 600
img = np.full((h, w), 215.0)
for y in (60, 200, 340): img[y, 30:570] = 60
for x in (100, 300, 500): img[20:380, x] = 60
for t in range(0, 300): img[50 + t, 150 + t] = 60
for t in range(0, 250): img[330 - t, 320 + t] = 60
img += rng.normal(0, 6, (h, w))
save("08_lines_dark_on_light.png", img)

# 9. Фигуры для перепадов яркости
h, w = 420, 620
yy, xx = np.mgrid[0:h, 0:w]
img = np.full((h, w), 110.0)
img[(xx - 140) ** 2 + (yy - 140) ** 2 < 95 ** 2] = 210
img[60:220, 280:440] = 40
tri = (yy > 260) & (yy < 390) & (np.abs(xx - 520) < (yy - 260) * 0.6)
img[tri] = 190
ring = ((xx - 200) ** 2 + (yy - 320) ** 2 < 75 ** 2) & ((xx - 200) ** 2 + (yy - 320) ** 2 > 40 ** 2)
img[ring] = 30
img[300:400, 330:420] = 160
save("09_edges_shapes.png", img)

# 10. Те же фигуры с шумом — сравнение Робертса, Превитта, Собела и Кэнни
save("10_edges_shapes_noisy.png", img + rng.normal(0, 22, (h, w)))

# 11. Большое изображение для проверки скорости и многопоточности
h, w = 3000, 4000
yy, xx = np.mgrid[0:h, 0:w]
light = 80 + 150 * (xx / w)
big = light.copy()
mask = text_mask(w, h, 48, margin=120, repeat=40)
big[mask] = light[mask] * 0.4
for cx, cy, r in [(900, 2300, 380), (2600, 2200, 450), (3500, 600, 300)]:
    big[(xx - cx) ** 2 + (yy - cy) ** 2 < r * r] = 30
big += rng.normal(0, 1.2, (h, w))
save("11_large_4000x3000.png", big)
for p in sorted(out.iterdir()): print(p.name, Image.open(p).size, p.stat().st_size // 1024, "KB")
