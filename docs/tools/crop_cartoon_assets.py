"""Crop cartoon-theme art from the course-reference screenshots (番茄ToDo, course demo only) into Android drawables.

usage: python docs/tools/crop_cartoon_assets.py   (needs Pillow and the screenshots in 素材/)
Coordinates below are in the 923x2000 display space of the 1179x2556 screenshots.
Outputs WebP files into res/drawable-nodpi plus a contact sheet for review.
"""
from collections import deque
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "素材"  # 本地参考截图，不随仓库提交
OUT = ROOT / "android/app/src/main/res/drawable-nodpi"
SHEET = Path("cartoon_assets_sheet.png")  # 输出到当前目录，供人工核对
SX, SY = 1179 / 923, 2556 / 2000
OUT.mkdir(parents=True, exist_ok=True)
made = []


def load(name):
    return Image.open(SRC / name).convert("RGBA")


def box(x0, y0, x1, y1):
    return (round(x0 * SX), round(y0 * SY), round(x1 * SX), round(y1 * SY))


def close(a, b, tol):
    return abs(a[0] - b[0]) <= tol and abs(a[1] - b[1]) <= tol and abs(a[2] - b[2]) <= tol


def knockout(im, tol=26):
    """Flood-fill background from every border pixel; the dark outlines of the art stop the fill."""
    im = im.copy()
    w, h = im.size
    p = im.load()
    seen = bytearray(w * h)
    q = deque()
    for x in range(w):
        q.append((x, 0)); q.append((x, h - 1))
    for y in range(h):
        q.append((0, y)); q.append((w - 1, y))
    while q:
        x, y = q.popleft()
        i = y * w + x
        if seen[i]:
            continue
        seen[i] = 1
        c = p[x, y]
        # background = light pixels close to any neighbouring background colour (handles gradients/grass)
        if c[0] + c[1] + c[2] < 3 * 150:
            continue
        p[x, y] = (c[0], c[1], c[2], 0)
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < w and 0 <= ny < h and not seen[ny * w + nx]:
                n = p[nx, ny]
                if close(n, c, tol):
                    q.append((nx, ny))
    return drop_specks(im)


def drop_specks(im, keep=0.03):
    """Remove small opaque islands (stray outline fragments) left at the crop edges."""
    w, h = im.size
    p = im.load()
    label = [0] * (w * h)
    sizes = {}
    n = 0
    for sy in range(h):
        for sx in range(w):
            if p[sx, sy][3] == 0 or label[sy * w + sx]:
                continue
            n += 1
            stack, count = [(sx, sy)], 0
            label[sy * w + sx] = n
            while stack:
                x, y = stack.pop(); count += 1
                for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                    if 0 <= nx < w and 0 <= ny < h and not label[ny * w + nx] and p[nx, ny][3]:
                        label[ny * w + nx] = n; stack.append((nx, ny))
            sizes[n] = count
    if sizes:
        limit = max(sizes.values()) * keep
        for y in range(h):
            for x in range(w):
                k = label[y * w + x]
                if k and sizes[k] < limit:
                    r, g, b_, _ = p[x, y]; p[x, y] = (r, g, b_, 0)
    return im.crop(im.getbbox() or (0, 0, w, h))


def circle(im):
    w, h = im.size
    mask = Image.new("L", (w * 4, h * 4), 0)
    ImageDraw.Draw(mask).ellipse((2, 2, w * 4 - 3, h * 4 - 3), fill=255)
    im = im.copy()
    im.putalpha(mask.resize((w, h), Image.LANCZOS))
    return im


def fade_top(im, portion=0.28):
    """Fade the sky into the page background so the scene has no hard top edge."""
    im = im.copy()
    w, h = im.size
    a = im.getchannel("A")
    fade = int(h * portion)
    ap = a.load()
    for y in range(fade):
        k = y / fade
        for x in range(w):
            ap[x, y] = int(ap[x, y] * k)
    im.putalpha(a)
    return im


def lighten_alpha(im, floor=232):
    """Turn near-white paper into transparency (for the already-faded calendar art)."""
    im = im.copy()
    p = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, _ = p[x, y]
            m = min(r, g, b)
            alpha = 255 if m < floor - 40 else max(0, min(255, int((255 - m) * 255 / (255 - floor + 1))))
            p[x, y] = (r, g, b, alpha)
    return im.crop(im.getbbox())


def save(img, name, max_w=None, quality=92):
    if max_w and img.width > max_w:
        img = img.resize((max_w, round(img.height * max_w / img.width)), Image.LANCZOS)
    img.save(OUT / f"{name}.webp", "WEBP", quality=quality, method=6)
    made.append((name, img))


picker = load("主题选择.png")
home = load("首页.png")
view = load("视图页背景.png")
previews = {"breeze": load("主题2.png"), "farm": load("主题3.png"), "spring": load("主题4.png")}

# ---- 场景插画（主题选择页的大卡片，分辨率最高、无按钮遮挡）----
save(fade_top(picker.crop(box(62, 982, 862, 1212))), "toon_cream_scene")
save(fade_top(picker.crop(box(72, 578, 851, 818))), "toon_spring_scene")
save(fade_top(picker.crop(box(62, 1422, 862, 1712))), "toon_breeze_scene")
# 农场只在预览里完整出现：截到加号按钮左侧
save(fade_top(previews["farm"].crop(box(141, 1352, 664, 1590))), "toon_farm_scene")

# ---- 默认主题：首页的大图标与加号 ----
for i, x in enumerate((92, 277, 461, 645, 829)):
    save(knockout(home.crop(box(x - 48, 1806, x + 48, 1886))), f"toon_cream_nav_{i}")
save(circle(home.crop(box(752, 1622, 886, 1756))), "toon_cream_fab")

# ---- 其他三套：预览里的底部图标与加号 ----
fab_box = {"breeze": (667, 1477, 763, 1573), "farm": (667, 1477, 763, 1573), "spring": (667, 1477, 763, 1573)}
for key, im in previews.items():
    for i, x in enumerate((202, 331, 461, 591, 721)):
        # 春日郊游的草地描边紧贴图标上沿，起点下移避免带入横线
        top = 1606 if key == "spring" else 1598
        save(knockout(im.crop(box(x - 36, top, x + 36, 1668))), f"toon_{key}_nav_{i}")
    save(circle(im.crop(box(*fab_box[key]))), f"toon_{key}_fab")

# ---- 视图页淡彩插画（默认主题）----
save(lighten_alpha(view.crop(box(200, 1418, 660, 1708))), "toon_cream_calendar")

# contact sheet for review
cols, cell = 6, 260
rows = (len(made) + cols - 1) // cols
sheet = Image.new("RGBA", (cols * cell, rows * (cell + 24)), (120, 120, 120, 255))
d = ImageDraw.Draw(sheet)
for k, (name, img) in enumerate(made):
    t = img.copy(); t.thumbnail((cell - 10, cell - 10))
    x, y = (k % cols) * cell, (k // cols) * (cell + 24)
    sheet.alpha_composite(t, (x + 5, y + 5))
    d.text((x + 5, y + cell + 4), f"{name} {img.size[0]}x{img.size[1]}", fill=(255, 255, 255, 255))
sheet.save(SHEET)
print(len(made), "assets;", sum((OUT / f"{n}.webp").stat().st_size for n, _ in made) // 1024, "KB")
