#!/usr/bin/env python3
"""Draws the mod's small GUI sprites into src/main/resources/assets/tidypockets/textures/gui/sprites/."""
from pathlib import Path
from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tidypockets/textures/gui/sprites"
OUT.mkdir(parents=True, exist_ok=True)

C = {
    ".": (0, 0, 0, 0),
    "K": (33, 33, 33, 255),       # outline
    "W": (255, 255, 255, 255),    # bevel light
    "S": (85, 85, 85, 255),       # bevel shadow
    "F": (198, 198, 198, 255),    # face
    "H": (222, 226, 255, 255),    # face, hovered
    "G": (63, 63, 63, 255),       # glyph
    "B": (70, 110, 200, 255),     # glyph accent, hovered
    "O": (40, 28, 16, 255), "Y": (255, 204, 64, 255), "L": (255, 236, 140, 255), "D": (196, 140, 32, 255),
    "P": (255, 255, 255, 110),    # puff
}

GLYPHS = {
    "sort": [
        ".........",
        ".GGGGGG..",
        ".........",
        ".GGGG....",
        ".........",
        ".GG...G..",
        ".....GGG.",
        "......G..",
        ".........",
    ],
    "deposit": [
        "....G....",
        "....G....",
        "..GGGGG..",
        "...GGG...",
        "....G....",
        ".G.....G.",
        ".G.....G.",
        ".GGGGGGG.",
        ".........",
    ],
    "restock": [
        "....G....",
        "...GGG...",
        "..GGGGG..",
        "....G....",
        "....G....",
        ".G.....G.",
        ".G.....G.",
        ".GGGGGGG.",
        ".........",
    ],
    "search": [
        "..GGG....",
        ".G...G...",
        ".G...G...",
        ".G...G...",
        "..GGG....",
        ".....G...",
        "......G..",
        ".......G.",
        ".........",
    ],
}


def button(name, glyph, hovered):
    img = Image.new("RGBA", (11, 11), C["."])
    face = C["H"] if hovered else C["F"]
    for y in range(11):
        for x in range(11):
            edge = x in (0, 10) or y in (0, 10)
            if edge:
                corner = (x in (0, 10)) and (y in (0, 10))
                img.putpixel((x, y), C["."] if corner else C["K"])
            elif x == 1 or y == 1:
                img.putpixel((x, y), C["W"])
            elif x == 9 or y == 9:
                img.putpixel((x, y), C["S"])
            else:
                img.putpixel((x, y), face)
    for gy, row in enumerate(glyph):
        for gx, ch in enumerate(row):
            if ch == "G" and 1 <= gx + 1 <= 9 and 1 <= gy + 1 <= 9:
                img.putpixel((gx + 1, gy + 1), C["B"] if hovered else C["G"])
    img.save(OUT / f"{name}{'_highlighted' if hovered else ''}.png")


for n, g in GLYPHS.items():
    button(n, g, False)
    button(n, g, True)

lock = ["..OOO..", ".OLLLO.", ".O...O.", "OOOOOOO", "OYLYYYO", "OYYOYDO", "OYDDDDO", "OOOOOOO"]
img = Image.new("RGBA", (7, 8))
for y, r in enumerate(lock):
    for x, ch in enumerate(r):
        img.putpixel((x, y), C[{"Y": "Y"}.get(ch, ch)] if ch != "." else C["."])
img.save(OUT / "lock.png")

# 4-frame puff for the sort pop: a small ring of white dust that grows and thins
for f in range(4):
    size = 16
    img = Image.new("RGBA", (size, size), C["."])
    r = 3 + f * 2
    a = [230, 170, 110, 50][f]
    for y in range(size):
        for x in range(size):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if abs(d - r) < 1.0 and (x + y + f) % 2 == 0:
                img.putpixel((x, y), (255, 255, 255, a))
    img.save(OUT / f"puff_{f}.png")
print("sprites written to", OUT)
