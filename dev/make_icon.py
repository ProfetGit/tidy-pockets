#!/usr/bin/env python3
"""Tidy Pockets icon v2: composes the Blockbench renders in dev/icon/frames/ (transparent 1600 px PNGs, one per frame;
scene and animation in dev/icon/build_scene.js, textures from dev/icon/draw_sprites.lua) on the flat leather background.

Loop (3.2 s, 25 fps, 80 frames): a messy heap of redstone, gold and diamond blocks pops, lands as three sorted towers,
holds, then hops back into the heap. v1 (the 2D inventory panel) is archived in dev/icon/v1/.

  python3 dev/make_icon.py               icon outputs
  python3 dev/make_icon.py --mod-icon    also write the in-game mod icon (ships in the jar)

Writes dev/icon/out/icon-animated.gif (256 px, must stay <= 256 KiB), dev/icon/out/icon-512.png (still),
dev/icon/contact.png (every frame) and dev/icon/contact_96.png (the GIF frames at Modrinth's 96 px)."""
import argparse
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageMath

ROOT = Path(__file__).resolve().parent.parent
ICON = ROOT / "dev" / "icon"
SPRITES = ICON / "sprites"
OUT = ICON / "out"      # not dist/: ./gradlew dist deletes that folder
MOD_ICON = ROOT / "src/main/resources/assets/tidypockets/icon.png"

FPS = 25
S = 512                 # composition size
GRID = 64               # background cells (x8)
GIF_SIZE = 256
GIF_LIMIT = 256 * 1024
STILL_FRAME = 44        # sorted towers, settled after the ta-da
CROP_PCT = 0.90
CROP_MARGIN = 1.10
CROP_LIFT = 0.0         # shift the crop up (fraction of its side) to give the flights headroom
OUTLINE = 13
BG = (122, 78, 50)
SHADOW = (94, 58, 36)
INK = (36, 20, 12)


def write_gif(frames: list, out: Path, fps: int, delta: bool = True) -> int:
    """RGB frames -> GIF with an exact palette (Pillow's quantize(palette=...) snaps close colours together).
    Slot 255 is transparent: (delta) every pixel unchanged since the previous frame. Without delta, Pillow crops each
    frame to the box that changed. Returns the colour count. (From FullGhastAhead/dev/make_icon.py.)"""
    used = sorted({c for f in frames for _, c in f.getcolors(f.width * f.height)})
    if len(used) > 255:
        sys.exit(f"{len(used)} colours - more than a GIF palette holds next to the transparent slot")
    for mul in ((a, b, c) for a in range(1, 64, 2) for b in range(1, 64, 2) for c in range(1, 64, 2)):
        hashes = [(r * mul[0] + g * mul[1] + b * mul[2]) & 255 for r, g, b in used]
        if len(set(hashes)) == len(used):
            break
    else:
        return write_gif_np(frames, out, fps, used, delta)
    lut = [0] * 256
    for i, h in enumerate(hashes):
        lut[h] = i
    pal = [v for c in used for v in c] + [0, 0, 0] * (255 - len(used)) + [255, 0, 255]
    gif, prev = [], None
    for f in frames:
        r, g, b = f.split()
        h = ImageMath.lambda_eval(lambda a: (a["r"] * mul[0] + a["g"] * mul[1] + a["b"] * mul[2]) & 255, r=r, g=g, b=b)
        idx = h.convert("L").point(lut)
        gif.append(finish(idx, prev, pal, delta))
        prev = idx
    save(gif, out, fps)
    return len(used)


def write_gif_np(frames, out, fps, used, delta):
    """Exact palette through a numpy lookup, for colour sets the 8-bit hash can't separate."""
    import numpy as np
    key = {(r << 16) | (g << 8) | b: i for i, (r, g, b) in enumerate(used)}
    pal = [v for c in used for v in c] + [0, 0, 0] * (255 - len(used)) + [255, 0, 255]
    keys = np.array(sorted(key)), None
    order = np.array([key[k] for k in keys[0]], dtype=np.uint8)
    gif, prev = [], None
    for f in frames:
        a = np.asarray(f, dtype=np.uint32)
        packed = (a[..., 0] << 16) | (a[..., 1] << 8) | a[..., 2]
        idx = Image.fromarray(order[np.searchsorted(keys[0], packed)], "L")
        gif.append(finish(idx, prev, pal, delta))
        prev = idx
    save(gif, out, fps)
    return len(used)


def finish(idx, prev, pal, delta):
    q = idx.copy()
    if delta and prev is not None:
        q.paste(255, mask=ImageChops.difference(idx, prev).point(lambda v: 255 if v == 0 else 0))
    p = Image.frombytes("P", q.size, q.tobytes())
    p.putpalette(pal)
    return p


def save(gif, out, fps):
    gif[0].save(out, save_all=True, append_images=gif[1:], duration=1000 // fps, loop=0, optimize=True, disposal=1,
                transparency=255)


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
    """Decode the GIF and compare every frame with its source composition (Pillow merges identical frames)."""
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
        out.paste(f.resize((cell, cell), Image.LANCZOS), (x, y))
        d.text((x + 2, y + 1), str(k), fill=(255, 255, 255))
    return out


def loop_crop(raw: list) -> tuple:
    """Square crop around the robust (10th..90th percentile) union of the frames' art boxes."""
    boxes = [im.getchannel("A").getbbox() for im in raw]
    lo = lambda vals: sorted(vals)[int(len(vals) * (1 - CROP_PCT))]
    hi = lambda vals: sorted(vals)[int(len(vals) * CROP_PCT) - 1]
    box = (lo([b[0] for b in boxes]), lo([b[1] for b in boxes]), hi([b[2] for b in boxes]), hi([b[3] for b in boxes]))
    side = int(max(box[2] - box[0], box[3] - box[1]) * CROP_MARGIN)
    cx, cy = (box[0] + box[2]) // 2, (box[1] + box[3]) // 2 - int(side * CROP_LIFT)
    return (cx - side // 2, cy - side // 2, cx - side // 2 + side, cy - side // 2 + side)


def background(ground: tuple) -> Image.Image:
    """Flat leather on the 64-cell grid with a ground-shadow ellipse (cx, cy, rx, ry in cells) under the art."""
    bg = Image.new("RGB", (GRID, GRID), BG)
    cx, cy, rx, ry = ground
    for y in range(GRID):
        for x in range(GRID):
            if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1:
                bg.putpixel((x, y), SHADOW)
    return bg.resize((S, S), Image.NEAREST).convert("RGBA")


def compose(art: Image.Image, bg: Image.Image) -> Image.Image:
    ink = Image.new("RGBA", art.size, INK + (0,))
    ink.putalpha(art.getchannel("A").point(lambda v: 255 if v else 0).filter(ImageFilter.MaxFilter(OUTLINE)))
    return Image.alpha_composite(Image.alpha_composite(bg, ink), art).convert("RGB")


def ground_from(art: Image.Image) -> tuple:
    """Shadow ellipse under the sorted towers: spans ~95% of the art width, centred on its bottom band."""
    b = art.getchannel("A").getbbox()
    c = S / GRID
    return ((b[0] + b[2]) / 2 / c, (b[3] - 14) / c, (b[2] - b[0]) * 0.5 / c, 5.5)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--mod-icon", action="store_true")
    a = ap.parse_args()
    files = sorted((ICON / "frames").glob("frame_*.png"))
    if not files:
        sys.exit("no frames in dev/icon/frames - render them from Blockbench first")
    raw = [Image.open(f).convert("RGBA") for f in files]
    crop = loop_crop(raw)
    arts = [im.crop(crop).resize((S, S), Image.NEAREST) for im in raw]
    bg = background(ground_from(arts[STILL_FRAME]))
    frames = [compose(art, bg) for art in arts]

    OUT.mkdir(parents=True, exist_ok=True)
    small = [f.resize((GIF_SIZE, GIF_SIZE), Image.NEAREST) for f in frames]
    gif = OUT / "icon-animated.gif"
    size = best_gif(small, gif)
    verify_gif(gif, small)
    frames[STILL_FRAME].save(OUT / "icon-512.png")
    if a.mod_icon:
        MOD_ICON.parent.mkdir(parents=True, exist_ok=True)
        frames[STILL_FRAME].resize((128, 128), Image.LANCZOS).save(MOD_ICON)
    sheet(frames, 128).save(ICON / "contact.png")
    sheet(small, 96).save(ICON / "contact_96.png")
    colours = len({c for f in small for _, c in f.getcolors(1 << 20)})
    print(f"crop {crop}; {gif.relative_to(ROOT)}: {size / 1024:.1f} KiB, {len(frames)} frames, {colours} colours")
    if size > GIF_LIMIT:
        sys.exit(f"GIF is {size / 1024:.1f} KiB, over Modrinth's 256 KiB icon limit")


if __name__ == "__main__":
    main()
