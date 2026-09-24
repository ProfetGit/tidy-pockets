#!/usr/bin/env python3
"""Tidy Pockets icon: a 2D pixel animation composed in Python from the Aseprite sprites in dev/icon/sprites
(dev/icon/draw_sprites.lua). No Blockbench: the subject is a flat inventory GUI.

Loop (3.2 s, 25 fps, 80 frames) on a 72x72 texel canvas: a messy 3x3 inventory, the mouse winds up and middle-clicks,
the stacks hop to their sorted slots in a diagonal wave and merge (5 + 9 grass -> 14), a tidy hold with twinkles, then
the panel shakes and everything tumbles back into the mess. All timing is in T, UNITS and the key tables below.

  python3 dev/make_icon.py [--bg NAME]      icon outputs
  python3 dev/make_icon.py --options        dev/icon/bg_options.png: every background with its GIF size

Writes dev/icon/out/icon-animated.gif (x4 = 288 px, must stay <= 256 KiB), dev/icon/out/icon-576.png (x8 still),
src/main/resources/assets/tidypockets/icon.png (x2 still, the in-game mod icon), dev/icon/contact.png (every frame)
and dev/icon/contact_96.png (the GIF frames at Modrinth's 96 px)."""
import argparse
import math
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageMath

ROOT = Path(__file__).resolve().parent.parent
ICON = ROOT / "dev" / "icon"
SPRITES = ICON / "sprites"
DIST = ICON / "out"      # not dist/: ./gradlew dist deletes that folder
MOD_ICON = ROOT / "src/main/resources/assets/tidypockets/icon.png"

FPS, LEN = 25, 3.2
FRAMES = round(FPS * LEN)
GRID = 72
GIF_SCALE, PNG_SCALE, MOD_SCALE = 4, 8, 2
GIF_LIMIT = 256 * 1024
STILL = 1.48              # tidy, settled, before the twinkles

PANEL = (5, 6)            # panel sprite top-left on the canvas (texels)
SLOT, EDGE, ITEM = 16, 3, 14
MOUSE = (48, 44)          # mouse sprite top-left at rest
MOUSE_SIZE = (16, 22)
WHEEL = (8, 7)            # scroll wheel centre inside the mouse sprite
SHADOW = (2, 2)           # drop shadow offset of the panel, mouse and items

# name: (sprite, drop shadow, outline)
BACKGROUNDS = {
    "leather": ("bg_leather", "#5E3A24", "#24140C"),
    "denim": ("bg_denim", "#284470", "#121B30"),
    "green": ("bg_green", "#2D5E38", "#112515"),
    "terracotta": ("bg_terracotta", "#914230", "#33140D"),
    "plum": ("bg_plum", "#522B55", "#1E0E20"),
}
DEFAULT_BG = "leather"

T = {
    "click": 0.68,        # the mouse slams down; wheel lit until wheel_off
    "wheel_off": 0.88,
    "shake": 2.28,        # the panel shakes; the mess-up hops start right after
    "twinkles": [(1.56, (0, 0)), (1.72, (1, 0)), (1.88, (2, 0))],
}
# item, count, messy slot, tidy slot, (sort takeoff s, frames, apex texels), (mess-up takeoff s, frames, apex)
UNITS = [
    ("apple", 1, (1, 0), (2, 0), (0.76, 6, 5), (2.36, 8, 5)),
    ("grass", 5, (0, 1), (0, 0), (0.80, 6, 4), (2.36, 8, 5)),
    ("apple", 2, (1, 1), (2, 0), (0.84, 7, 4), (2.40, 8, 5)),
    ("grass", 9, (0, 2), (0, 0), (0.88, 8, 4), (2.40, 9, 4)),
    ("gold", 2, (2, 1), (0, 1), (0.96, 8, 4), (2.32, 9, 8)),
    ("pickaxe", 1, (1, 2), (1, 0), (0.92, 8, 4), (2.32, 9, 5)),
]
# mouse: (time, (dy, sx, sy), ease into this key); scale is anchored at the sprite's bottom centre
MOUSE_KEYS = [
    (0.40, (0, 1, 1)),
    (0.60, (-4, 0.94, 1.08), "out"),
    (0.64, (-4, 0.94, 1.08), "lin"),
    (0.68, (0, 1.16, 0.84), "step"),
    (0.72, (0, 0.92, 1.08), "step"),
    (0.76, (0, 1.04, 0.97), "step"),
    (0.80, (0, 1, 1), "step"),
]
SHAKE = [(2, 0), (-2, 0), (2, 0), (-1, 0), (1, 0)]    # panel offset per frame from T["shake"]
THUMP = [(0, 1), (0, 1)]                              # panel offset per frame from T["click"]
LAND = [(1.3, 0.72), (0.86, 1.14), (1.06, 0.95)]     # squash per frame after touchdown in an empty slot
MERGE = [(1.24, 0.8), (0.92, 1.1), (1.03, 0.97)]     # touchdown onto a stack of the same item
ANTIC = (1.18, 0.82)                                  # the frame before takeoff
FX = {  # per-frame scales
    "star": [0.8, 0.5], "ring": [0.45, 0.75, 1.0, 1.2], "puff": [0.45, 0.65, 0.5, 0.3],
    "spark": [0.5, 1.0, 0.7, 0.35], "twinkle": [0.45, 0.85, 0.55, 0.25],
}
COUNT, COUNT_SHADOW, COUNT_FLASH = (255, 255, 255, 255), (63, 63, 63, 255), (255, 225, 77, 255)

EASE = {
    "lin": lambda u: u,
    "in": lambda u: u * u,
    "out": lambda u: 1 - (1 - u) ** 2,
    "smooth": lambda u: u * u * (3 - 2 * u),
    "step": lambda u: 0.0,
}
_cache = {}


def sprite(name: str) -> Image.Image:
    if name not in _cache:
        _cache[name] = Image.open(SPRITES / f"{name}.png").convert("RGBA")
    return _cache[name]


def F(t: float) -> int:
    return round(t * FPS)


def lerp(a, b, u):
    if isinstance(a, tuple):
        return tuple(x + (y - x) * u for x, y in zip(a, b))
    return a + (b - a) * u


def track(i: int, keys: list):
    """Sample keys [(time, value[, ease into this key])] at frame i; values hold before the first and after the last."""
    if i <= F(keys[0][0]):
        return keys[0][1]
    for a, b in zip(keys, keys[1:]):
        fa, fb = F(a[0]), F(b[0])
        if i < fb:
            return lerp(a[1], b[1], EASE[b[2] if len(b) > 2 else "smooth"]((i - fa) / (fb - fa)))
    return keys[-1][1]


def panel_offset(i: int) -> tuple:
    for start, seq in ((F(T["click"]), THUMP), (F(T["shake"]), SHAKE)):
        if start <= i < start + len(seq):
            return seq[i - start]
    return (0, 0)


def slot_foot(slot: tuple, off=(0, 0)) -> tuple:
    """Bottom centre of the item in a slot (texels)."""
    c, r = slot
    return (PANEL[0] + EDGE + c * SLOT + 1 + ITEM / 2 + off[0], PANEL[1] + EDGE + r * SLOT + 1 + ITEM + off[1])


def hop(p0: tuple, p1: tuple, apex: float, u: float) -> tuple:
    """Projectile from p0 to p1 (y down) whose top is `apex` above the higher end; u is the time fraction."""
    top = min(p0[1], p1[1]) - apex
    a, b = p0[1] - top, p1[1] - top
    g = (math.sqrt(2 * a) + math.sqrt(2 * b)) ** 2
    v = math.sqrt(2 * g * a)
    return p0[0] + (p1[0] - p0[0]) * u, p0[1] - v * u + g * u * u / 2, (-v + g * u) / max(v, 1e-6)


class Canvas:
    """Draws texel-space sprites onto an RGBA layer at an integer scale (nearest neighbour, binary alpha)."""

    def __init__(self, scale: int, size=(GRID, GRID)):
        self.s = scale
        self.art = Image.new("RGBA", (size[0] * scale, size[1] * scale), (0, 0, 0, 0))
        self.body = Image.new("L", self.art.size, 0)   # what casts the drop shadow (no FX)

    def draw(self, img: Image.Image, at: tuple, scale=(1, 1), anchor=(0.5, 1.0), body=True):
        w, h = max(1, round(img.width * self.s * scale[0])), max(1, round(img.height * self.s * scale[1]))
        big = img.resize((w, h), Image.NEAREST)
        x, y = round(at[0] * self.s - anchor[0] * w), round(at[1] * self.s - anchor[1] * h)
        a = big.getchannel("A")
        self.art.paste(big, (x, y), a)
        if body:
            self.body.paste(255, (x, y), a)

    def count(self, n: int, left: float, top: float, flash=False):
        """Vanilla-style stack count at the bottom right of a 14x14 item whose top-left is (left, top)."""
        if n < 2:
            return
        s, digits = str(n), sprite("digits")
        x0, y0 = left + ITEM - 1 - (len(s) * 4 - 1), top + ITEM - 6   # the shadow stays off the slot border
        for dx, colour in ((1, COUNT_SHADOW), (0, COUNT_FLASH if flash else COUNT)):
            for k, ch in enumerate(s):
                g = digits.crop((int(ch) * 3, 0, int(ch) * 3 + 3, 5)).resize((3 * self.s, 5 * self.s), Image.NEAREST)
                pos = (round((x0 + k * 4 + dx) * self.s), round((y0 + dx) * self.s))
                self.art.paste(Image.new("RGBA", g.size, colour), pos, g.getchannel("A"))


def unit_state(u: tuple, i: int):
    """('rest', slot, landed_frame) or ('air', src, dst, takeoff_frame, frames, apex, tumble) for a unit at frame i."""
    _, _, messy, tidy, (st, sn, sa), (mt, mn, ma) = u
    s0, m0 = F(st), F(mt)
    if i < s0:
        return ("rest", messy, m0 + mn - FRAMES)
    if i < s0 + sn:
        return ("air", messy, tidy, s0, sn, sa, False)
    if i < m0:
        return ("rest", tidy, s0 + sn)
    if i < m0 + mn:
        return ("air", tidy, messy, m0, mn, ma, True)
    return ("rest", messy, m0 + mn)


def mouse_pose(i: int) -> tuple:
    dy, sx, sy = track(i, MOUSE_KEYS)
    return (MOUSE[0] + MOUSE_SIZE[0] / 2, MOUSE[1] + MOUSE_SIZE[1] + dy), (sx, sy)


def wheel_point(i: int) -> tuple:
    foot, (sx, sy) = mouse_pose(i)
    return foot[0] + (WHEEL[0] - MOUSE_SIZE[0] / 2) * sx, foot[1] - (MOUSE_SIZE[1] - WHEEL[1]) * sy


def fx_events() -> list:
    """(sprite, first frame, per-frame scales, centre texel point, drawn under flying items)"""
    c = F(T["click"])
    ev = [("fx_star", c, FX["star"], wheel_point(c), False), ("fx_ring", c, FX["ring"], wheel_point(c), False)]
    for u in UNITS:
        foot = slot_foot(u[2])
        ev.append(("fx_puff", F(u[4][0]), FX["puff"], (foot[0], foot[1] - ITEM / 2), True))
    for k, u in enumerate(UNITS):     # merges: a later unit landing where an earlier one with the same tidy slot rests
        if any(v[3] == u[3] and F(v[4][0]) + v[4][1] < F(u[4][0]) + u[4][1] for v in UNITS[:k] + UNITS[k + 1:]):
            foot = slot_foot(u[3])
            ev.append(("fx_spark", F(u[4][0]) + u[4][1], FX["spark"], (foot[0] + 5, foot[1] - ITEM + 2), False))
    for t, slot in T["twinkles"]:
        foot = slot_foot(slot)
        ev.append(("fx_spark", F(t), FX["twinkle"], (foot[0] - 4, foot[1] - ITEM + 3), False))
    return ev


FX_EVENTS = fx_events()


def render(i: int, scale: int) -> Canvas:
    """Art for frame i (transparent background)."""
    cv = Canvas(scale)
    off = panel_offset(i)
    cv.draw(sprite("panel"), (PANEL[0] + off[0], PANEL[1] + off[1]), anchor=(0, 0))

    states = [unit_state(u, i) for u in UNITS]
    slots = {}
    for u, st in zip(UNITS, states):
        if st[0] == "rest":
            slots.setdefault(st[1], []).append((u, st[2]))
    for slot, rest in slots.items():
        latest = max(f for _, f in rest)
        merged = any(f < latest for _, f in rest)
        age = i - latest
        squash, flash = (1, 1), False
        if 0 <= age < len(LAND):
            squash, flash = (MERGE if merged else LAND)[age], merged and age < 2
        elif any(F(u[4][0]) - 1 == i or F(u[5][0]) - 1 == i for u, _ in rest):
            squash = ANTIC
        foot = slot_foot(slot, off)
        cv.draw(sprite(rest[0][0][0]), foot, squash)
        cv.count(sum(u[1] for u, _ in rest), foot[0] - ITEM / 2, foot[1] - ITEM, flash)

    for name, f0, scales, at, under in FX_EVENTS:
        if under and 0 <= i - f0 < len(scales):
            cv.draw(sprite(name), at, (scales[i - f0],) * 2, (0.5, 0.5), body=False)

    foot, sc = mouse_pose(i)
    lit = F(T["click"]) <= i < F(T["wheel_off"])
    cv.draw(sprite("mouse_click" if lit else "mouse"), foot, sc)

    for u, st in zip(UNITS, states):
        if st[0] != "air":
            continue
        _, src, dst, f0, n, apex, tumble = st
        p0, p1 = slot_foot(src, panel_offset(f0)), slot_foot(dst, panel_offset(f0 + n))
        q = (i - f0) / n
        x, y, vy = hop(p0, p1, apex, q)
        k = min(1.0, abs(vy))
        lift = 1 + 0.12 * math.sin(math.pi * q)
        img = sprite(u[0])
        if tumble:
            turns = int(q * 4) % 4
            img = img.rotate(90 * turns * (-1 if p1[0] >= p0[0] else 1))
        cv.draw(img, (x, y), ((1 - 0.1 * k) * lift, (1 + 0.14 * k) * lift))
        cv.count(u[1], x - ITEM / 2, y - ITEM)

    for name, f0, scales, at, under in FX_EVENTS:
        if not under and 0 <= i - f0 < len(scales):
            cv.draw(sprite(name), at, (scales[i - f0],) * 2, (0.5, 0.5), body=False)
    return cv


def dilate(mask: Image.Image, r: int) -> Image.Image:
    out = mask.copy()
    for dx in (-r, 0, r):
        for dy in (-r, 0, r):
            if dx or dy:
                shifted = Image.new("L", mask.size, 0)
                shifted.paste(mask, (dx, dy))
                out = ImageChops.lighter(out, shifted)
    return out


def compose(cv: Canvas, bg_name: str, origin=(0, 0), background: Image.Image | None = None) -> Image.Image:
    """Background, drop shadow, one-texel dark outline, art. origin (px) places the art canvas on a larger background."""
    spr, shadow, outline = BACKGROUNDS[bg_name]
    s = cv.s
    if background is None:
        background = sprite(spr).resize((GRID * s, GRID * s), Image.NEAREST)
    out = background.convert("RGB")
    body = Image.new("L", cv.body.size, 0)
    body.paste(cv.body, (SHADOW[0] * s, SHADOW[1] * s))
    out.paste(shadow, origin, body)
    alpha = cv.art.getchannel("A")
    out.paste(outline, origin, dilate(alpha, s))
    out.paste(cv.art.convert("RGB"), origin, alpha)
    return out


def write_gif(frames: list, out: Path, fps: int, key_mask: Image.Image | None = None, delta: bool = True) -> int:
    """Write RGB frames as a GIF with an exact palette (Pillow's quantize(palette=...) snaps close colours together).
    Slot 255 is transparent: the key_mask pixels in every frame, plus (delta) every pixel unchanged since the previous frame
    (disposal 1 keeps what is underneath). Without delta, Pillow crops each frame to the box that changed.
    Returns the colour count. (Copied from FullGhastAhead/dev/make_icon.py.)"""
    used = sorted({c for f in frames for _, c in f.getcolors(f.width * f.height)})
    if len(used) > 255:
        sys.exit(f"{len(used)} colours - more than a GIF palette holds next to the transparent slot")
    for mul in ((a, b, c) for a in range(1, 64, 2) for b in range(1, 64, 2) for c in range(1, 64, 2)):
        hashes = [(r * mul[0] + g * mul[1] + b * mul[2]) & 255 for r, g, b in used]
        if len(set(hashes)) == len(used):
            break
    else:
        sys.exit("no collision-free colour hash found")
    lut = [0] * 256
    for i, h in enumerate(hashes):
        lut[h] = i
    pal = [v for c in used for v in c] + [0, 0, 0] * (255 - len(used)) + [255, 0, 255]
    gif, prev = [], None
    for f in frames:
        r, g, b = f.split()
        h = ImageMath.lambda_eval(lambda a: (a["r"] * mul[0] + a["g"] * mul[1] + a["b"] * mul[2]) & 255, r=r, g=g, b=b)
        idx = h.convert("L").point(lut)
        q = idx.copy()
        if key_mask is not None:
            q.paste(255, mask=key_mask)
        if delta and prev is not None:
            q.paste(255, mask=ImageChops.difference(idx, prev).point(lambda v: 255 if v == 0 else 0))
        prev = idx
        p = Image.frombytes("P", q.size, q.tobytes())
        p.putpalette(pal)
        gif.append(p)
    gif[0].save(out, save_all=True, append_images=gif[1:], duration=1000 // fps, loop=0, optimize=True, disposal=1,
                transparency=255)
    return len(used)


def best_gif(frames: list, out: Path) -> int:
    """Write whichever of the delta / changed-box encodings is smaller; returns its size in bytes."""
    best = None
    for delta in (False, True):
        buf = out.with_suffix(".tmp.gif")
        write_gif(frames, buf, FPS, delta=delta)
        size = buf.stat().st_size
        if best is None or size < best:
            best = size
            buf.replace(out)
        else:
            buf.unlink()
    return best


def verify_gif(path: Path, frames: list):
    """Decode the GIF and compare every frame with its source composition."""
    im = Image.open(path)
    got = []
    for k in range(im.n_frames):
        im.seek(k)
        got.extend([im.convert("RGB")] * max(1, round(im.info.get("duration", 40) / (1000 / FPS))))
    if len(got) != len(frames):
        sys.exit(f"{path.name}: {len(got)} decoded frames, expected {len(frames)}")
    for k, (a, b) in enumerate(zip(got, frames)):
        if ImageChops.difference(a, b).getbbox():
            sys.exit(f"{path.name}: frame {k} differs from its source")


def sheet(frames: list, cell: int, cols: int = 10, pad: int = 4) -> Image.Image:
    rows = (len(frames) + cols - 1) // cols
    out = Image.new("RGB", (cols * (cell + pad) + pad, rows * (cell + pad) + pad), (20, 20, 28))
    d = ImageDraw.Draw(out)
    for k, f in enumerate(frames):
        x, y = pad + (k % cols) * (cell + pad), pad + (k // cols) * (cell + pad)
        out.paste(f.resize((cell, cell), Image.NEAREST if cell % GRID == 0 else Image.LANCZOS), (x, y))
        d.text((x + 2, y + 1), str(k), fill=(255, 255, 255))
    return out


def icon(bg_name: str):
    frames = [compose(render(i, GIF_SCALE), bg_name) for i in range(FRAMES)]
    DIST.mkdir(parents=True, exist_ok=True)
    gif = DIST / "icon-animated.gif"
    size = best_gif(frames, gif)
    verify_gif(gif, frames)
    still = F(STILL)
    compose(render(still, PNG_SCALE), bg_name).save(DIST / f"icon-{GRID * PNG_SCALE}.png")
    MOD_ICON.parent.mkdir(parents=True, exist_ok=True)
    compose(render(still, MOD_SCALE), bg_name).save(MOD_ICON)
    sheet(frames, GRID * 2).save(ICON / "contact.png")
    sheet(frames, 96).save(ICON / "contact_96.png")
    colours = len({c for f in frames for _, c in f.getcolors(1 << 20)})
    print(f"{gif.relative_to(ROOT)}: {size / 1024:.1f} KiB, {len(frames)} frames, {colours} colours, bg {bg_name}")
    if size > GIF_LIMIT:
        sys.exit(f"GIF is {size / 1024:.1f} KiB, over Modrinth's 256 KiB icon limit")


def options():
    """Every background: 3 frames at x2 plus the whole loop at 96 px on dark and light backdrops, with the GIF size."""
    picks = [0, F(T["click"]), F(1.08), F(STILL)]
    rows = []
    for name in BACKGROUNDS:
        frames = [compose(render(i, GIF_SCALE), name) for i in range(FRAMES)]
        buf = ICON / f".options_{name}.gif"
        size = best_gif(frames, buf)
        buf.unlink()
        row = Image.new("RGB", (4 * (GRID * 2 + 8) + 2 * 104 + 8, GRID * 2 + 28), (20, 20, 28))
        for k, i in enumerate(picks):
            row.paste(frames[i].resize((GRID * 2, GRID * 2), Image.NEAREST), (8 + k * (GRID * 2 + 8), 20))
        for k, back in enumerate(((20, 20, 28), (236, 236, 240))):
            box = Image.new("RGB", (104, 104), back)
            box.paste(frames[F(STILL)].resize((96, 96), Image.LANCZOS), (4, 4))
            row.paste(box, (8 + 4 * (GRID * 2 + 8) + k * 104, 20))
        ImageDraw.Draw(row).text((8, 4), f"{name}: GIF {size / 1024:.0f} KiB", fill=(230, 230, 240))
        rows.append(row)
        print(f"{name}: {size / 1024:.1f} KiB")
    out = Image.new("RGB", (rows[0].width, sum(r.height for r in rows)), (20, 20, 28))
    for k, r in enumerate(rows):
        out.paste(r, (0, k * r.height))
    out.save(ICON / "bg_options.png")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--bg", default=DEFAULT_BG, choices=sorted(BACKGROUNDS))
    ap.add_argument("--options", action="store_true")
    a = ap.parse_args()
    if a.options:
        options()
    else:
        icon(a.bg)


if __name__ == "__main__":
    main()
