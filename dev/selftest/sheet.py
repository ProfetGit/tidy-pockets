#!/usr/bin/env python3
"""Contact sheet of captured frames: sheet.py <frames-dir> <out.png> [x0 y0 x1 y1] [every-nth] [cols]"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw

src, out = Path(sys.argv[1]), sys.argv[2]
box = tuple(int(v) for v in sys.argv[3:7]) if len(sys.argv) >= 7 else None
nth = int(sys.argv[7]) if len(sys.argv) > 7 else 1
cols = int(sys.argv[8]) if len(sys.argv) > 8 else 6
frames = sorted(src.glob("*.png"))[::nth]
tiles = []
for f in frames:
    im = Image.open(f).convert("RGB")
    if box:
        im = im.crop(box)
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 70, 12), fill=(0, 0, 0))
    d.text((2, 1), f.stem.split("_")[-1], fill=(255, 255, 0))
    tiles.append(im)
w, h = tiles[0].size
rows = (len(tiles) + cols - 1) // cols
sheet = Image.new("RGB", (cols * (w + 4), rows * (h + 4)), (255, 0, 255))
for i, t in enumerate(tiles):
    sheet.paste(t, ((i % cols) * (w + 4), (i // cols) * (h + 4)))
sheet.save(out)
print(out, len(tiles), "frames")
