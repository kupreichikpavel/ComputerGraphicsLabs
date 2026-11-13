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
        "уточняет порог как среднее двух средних. Нелинейные фильтры упорядочивают "
        "значения в окне и выбирают элемент с нужным номером.")

def save(name, arr):
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "L").convert("RGB").save(out / name, optimize=True)

def text_mask(w, h, size, margin=30, text=TEXT, repeat=3, font=FONT):
    img = Image.new("L", (w, h), 0); d = ImageDraw.Draw(img); f = ImageFont.truetype(font, size)
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

def impulses(img, density, salt_share=0.5):
    out = img.copy()
    hit = rng.random(img.shape) < density
    salt = rng.random(img.shape) < salt_share
    out[hit & salt] = 255
    out[hit & ~salt] = 0
    return out

def scene(w, h):
    yy, xx = np.mgrid[0:h, 0:w]
    img = 70 + 110 * xx / w + 15 * np.sin(yy / 35)
    img[(xx - w * 0.2) ** 2 + (yy - h * 0.32) ** 2 < (h * 0.2) ** 2] = 215
    img[int(h * 0.12):int(h * 0.48), int(w * 0.42):int(w * 0.62)] = 35
    tri = (yy > h * 0.58) & (yy < h * 0.92) & (np.abs(xx - w * 0.2) < (yy - h * 0.58) * 0.7)
    img[tri] = 185
    ring = ((xx - w * 0.82) ** 2 + (yy - h * 0.3) ** 2 < (h * 0.17) ** 2) & ((xx - w * 0.82) ** 2 + (yy - h * 0.3) ** 2 > (h * 0.09) ** 2)
    img[ring] = 60
    for k, x in enumerate(range(int(w * 0.42), int(w * 0.95), 22)):
        img[int(h * 0.6):int(h * 0.92), x:x + 1 + k % 3] = 20
    for k in range(6):
        cx, cy = int(w * (0.44 + 0.08 * k)), int(h * 0.53)
        img[cy - 1:cy + 2, cx - 1:cx + 2] = 245
    pil = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "L")
    d = ImageDraw.Draw(pil)
    d.text((int(w * 0.04), int(h * 0.02)), "Медиана сохраняет края", fill=20, font=ImageFont.truetype(FONT, max(16, h // 22)))
    return np.array(pil).astype(float)

def particles(w, h, sign):
    yy, xx = np.mgrid[0:h, 0:w]
    img = 40 + 150 * np.exp(-(((xx - 180) / 260) ** 2 + ((yy - 160) / 200) ** 2)) + 40 * xx / w
    if sign < 0:
        img = 255 - img
    for _ in range(90):
        cx, cy = rng.integers(10, w - 10), rng.integers(10, h - 10)
        r = rng.uniform(1.5, 4.0)
        img[(xx - cx) ** 2 + (yy - cy) ** 2 <= r * r] += 45 * sign
    return img + rng.normal(0, 3, (h, w))

# 6. Импульсный шум 10% — медиана
clean = scene(640, 440)
save("06_salt_pepper.png", impulses(clean, 0.10))

# 7. Только «соль» — фильтр минимума
save("07_salt_only.png", impulses(clean, 0.08, 1.0))

# 8. Только «перец» — фильтр максимума
save("08_pepper_only.png", impulses(clean, 0.08, 0.0))

# 9. Светлые частицы на неоднородном фоне — Top-Hat
save("09_tophat_particles.png", particles(640, 440, 1))

# 10. Тёмные пятна на неоднородном фоне — Black-Hat
save("10_blackhat_spots.png", particles(640, 440, -1))

# 11. Фигуры — морфологический градиент
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
save("11_gradient_shapes.png", img + rng.normal(0, 2, (h, w)))

# 12. Зашумлённый документ — медиана, затем порог Оцу
h, w = 420, 640
mask = text_mask(w, h, 24, font="/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")
save("12_noisy_document.png", impulses(np.where(mask, 40.0, 225.0) + rng.normal(0, 4, (h, w)), 0.12))

# 13. Большое изображение для проверки скорости и многопоточности
h, w = 3000, 4000
yy, xx = np.mgrid[0:h, 0:w]
light = 80 + 150 * (xx / w)
big = light.copy()
mask = text_mask(w, h, 48, margin=120, repeat=40)
big[mask] = light[mask] * 0.4
for cx, cy, r in [(900, 2300, 380), (2600, 2200, 450), (3500, 600, 300)]:
    big[(xx - cx) ** 2 + (yy - cy) ** 2 < r * r] = 30
save("13_large_4000x3000.png", impulses(big, 0.10))
for p in sorted(out.iterdir()): print(p.name, Image.open(p).size, p.stat().st_size // 1024, "KB")
