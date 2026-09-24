#!/usr/bin/env python3
"""Compose the Modrinth description banner (3:1, 1536x512) around the icon animation from make_icon.py.

Layers: sprites/banner_bg x8 (leather with a stitched seam), the icon art rendered at x6 with its top-left at ART_ORIGIN,
banner_title x8 and banner_tagline x6 (all drawn by dev/icon/draw_sprites.lua).
Writes dev/icon/out/banner.png (frame STILL, alpha corners) and dev/icon/out/banner-animated.gif (the whole loop, exact palette; the
stepped corners and every pixel unchanged since the previous frame use the transparent slot). Copy the GIF to
docs/banner.gif for GitHub. --stills writes dev/icon/banner_stills.png to compare still-frame candidates."""
import argparse
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw

import make_icon as M

W, H = 1536, 512
GRID = 8
ART_SCALE = 6
ART_ORIGIN = (1014, 40)    # banner px of the art canvas's top-left: rest pose centred at about x 1230, y 256
LAYERS = (("banner_title", 8, (72, 152)), ("banner_tagline", 6, (74, 304)))
STILL = 30
CANDIDATES = (17, 22, 24, 26, 30, 40)
GIF_LIMIT = 5 * 1024 * 1024
CORNER = (4, 2, 1, 1)      # pixel-art rounded corners: cells cut per row on the 8 px grid; symmetric, so rows = columns


def scaled(name: str, scale: int) -> Image.Image:
    im = M.sprite(name)
    return im.resize((im.width * scale, im.height * scale), Image.NEAREST)


def corner_mask() -> Image.Image:
    """255 inside the banner, 0 in the stepped corners."""
    gw, gh = W // GRID, H // GRID
    m = Image.new("L", (gw, gh), 255)
    for row, cut in enumerate(CORNER):
        for x in range(cut):
            for p in ((x, row), (gw - 1 - x, row), (x, gh - 1 - row), (gw - 1 - x, gh - 1 - row)):
                m.putpixel(p, 0)
    return m.resize((W, H), Image.NEAREST)


def base() -> Image.Image:
    out = scaled("banner_bg", GRID).convert("RGB")
    for name, scale, pos in LAYERS:
        im = scaled(name, scale)
        out.paste(im.convert("RGB"), pos, im.getchannel("A"))
    return out


def frame(i: int, back: Image.Image) -> Image.Image:
    return M.compose(M.render(i, ART_SCALE), M.DEFAULT_BG, ART_ORIGIN, back)


def verify(path: Path, frames: list, mask: Image.Image):
    """Every decoded frame matches its source inside the mask and is transparent in the corners."""
    im = Image.open(path)
    got = []
    for k in range(im.n_frames):
        im.seek(k)
        got.extend([im.convert("RGBA")] * max(1, round(im.info.get("duration", 40) / (1000 / M.FPS))))
    if len(got) != len(frames):
        sys.exit(f"{path.name}: {len(got)} decoded frames, expected {len(frames)}")
    black = Image.new("RGB", (W, H))
    for k, (g, f) in enumerate(zip(got, frames)):
        inside = Image.composite(g.convert("RGB"), black, mask)
        if ImageChops.difference(inside, Image.composite(f, black, mask)).getbbox():
            sys.exit(f"{path.name}: frame {k} differs from its source")
        if ImageChops.multiply(g.getchannel("A"), mask.point(lambda v: 255 - v)).getbbox():
            sys.exit(f"{path.name}: frame {k} has opaque corner pixels")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--stills", action="store_true")
    a = ap.parse_args()
    back = base()
    if a.stills:
        cells = [frame(i, back).resize((W // 3, H // 3), Image.LANCZOS) for i in CANDIDATES]
        out = Image.new("RGB", (2 * (W // 3) + 12, 3 * (H // 3 + 4) + 4), (20, 20, 28))
        d = ImageDraw.Draw(out)
        for k, (i, c) in enumerate(zip(CANDIDATES, cells)):
            x, y = 4 + (k % 2) * (W // 3 + 4), 4 + (k // 2) * (H // 3 + 4)
            out.paste(c, (x, y))
            d.text((x + 6, y + 4), f"frame {i}", fill=(255, 255, 255))
        out.save(M.ICON / "banner_stills.png")
        return

    frames = [frame(i, back) for i in range(M.FRAMES)]
    mask = corner_mask()
    M.DIST.mkdir(parents=True, exist_ok=True)
    still = frames[STILL].convert("RGBA")
    still.putalpha(mask)
    still.save(M.DIST / "banner.png", optimize=True)

    out = M.DIST / "banner-animated.gif"
    colours = M.write_gif(frames, out, M.FPS, key_mask=mask.point(lambda v: 255 - v), delta=True)
    verify(out, frames, mask)
    size = out.stat().st_size
    print(f"{(M.DIST / 'banner.png').relative_to(M.ROOT)} (frame {STILL}); {out.relative_to(M.ROOT)}: {size / 1024:.0f} KiB, {len(frames)} frames, "
          f"{colours} colours ({'OK' if size <= GIF_LIMIT else 'OVER'} Modrinth's 5 MiB gallery limit)")
    if size > GIF_LIMIT:
        sys.exit(1)


if __name__ == "__main__":
    main()
