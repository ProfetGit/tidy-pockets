#!/usr/bin/env python3
"""Look mockups for the settings previews (the sort scene), drawn from the game's own textures with the live-preview
skill's mockgui, so the director can pick a look before any scene is built.
usage: python3 -I dev/preview/mockup.py   ->  dev/preview/out/mockups.webp, mockups-sheet.png, mockups-still.png"""
import math
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path.home() / ".claude/skills/live-preview/scripts"))
import numpy as np  # noqa: E402
from mockgui import MC, W, Box, animate, clamp, count, item, smooth, soft_shadow, tex  # noqa: E402

MOD = Path(__file__).resolve().parents[2]
CLICK, PERIOD = 800, 2600
ITEMS = {"pickaxe": "iron_pickaxe", "sword": "stone_sword", "arrow": "arrow", "apple": "apple", "bread": "bread",
         "beef": "cooked_beef", "coal": "coal", "iron": "iron_ingot", "gold": "gold_ingot", "diamond": "diamond",
         "pearl": "ender_pearl"}
BLOCKS = {"log": ("oak_log_top", "oak_log"), "cobble": ("cobblestone", "cobblestone"), "dirt": ("dirt", "dirt")}
ORDER = ["log", "cobble", "dirt", "pickaxe", "sword", "arrow", "apple", "bread", "beef", "coal", "iron", "gold",
         "diamond", "pearl"]
STACKS = [("cobble", 23), ("apple", 5), ("log", 32), ("pickaxe", 1), ("gold", 12), ("cobble", 38), ("bread", 9),
          ("arrow", 24), ("dirt", 51), ("diamond", 3), ("apple", 7), ("coal", 18), ("sword", 1), ("iron", 27),
          ("beef", 14), ("pearl", 6), ("log", 11), ("coal", 30)]


def thing(box, kind, cx, cy, k=1.0, scale=1.0):
    item(box, ITEMS.get(kind), cx, cy, k, scale, BLOCKS.get(kind))


# ---- the mod's sort pop (anim/SortPop, SlotAnims) at speed 1 ----
def out_back(t):
    u = t - 1
    return 1 + 2.9 * u * u * u + 1.9 * u * u


def pop_scale(t, col, row):
    p = (t - CLICK - min(80, (row + col) * 6)) / 90
    return 0.0 if p < 0 else max(0.0, out_back(p)) if p < 1 else 1.0


def puff(box, t, col, row, cx, cy, scale=1.0):
    p = (t - CLICK - min(80, (row + col) * 6) + 40) / 80
    if 0 <= p < 1:
        art = MOD / f"src/main/resources/assets/tidypockets/textures/gui/sprites/puff_{int(p * 4)}.png"
        box.sprite(tex(art), cx - 8 * scale, cy - 8 * scale, 16 * scale, 16 * scale)


def inventory(cols, rows, stacks, seed):
    slots = random.Random(seed).sample(range(cols * rows), len(stacks))
    mess = dict(zip(slots, stacks))
    total = {}
    for kind, n in stacks:
        total[kind] = total.get(kind, 0) + n
    tidy, i = {}, 0
    for kind in ORDER:
        n = total.get(kind, 0)
        while n > 0:
            take = min(n, 1 if kind in ("pickaxe", "sword") else 16 if kind == "pearl" else 64)
            tidy[i] = (kind, take)
            n -= take
            i += 1
    return mess, tidy


# the inventory's items over whatever holds them: at(i) is slot i's centre
def contents(box, t, cols, rows, mess, tidy, at, scale=1.0, shadows=False):
    states = []
    for i in range(cols * rows):
        moved = t >= CLICK and mess.get(i) != tidy.get(i)
        stack = (tidy if t >= CLICK else mess).get(i)
        k = pop_scale(t, i % cols, i // cols) if moved else 1.0
        states.append((stack, k, moved))
    for i, (stack, k, _) in enumerate(states):
        cx, cy = at(i)
        if shadows:
            soft_shadow(box, cx, cy + 13, 14, 5, 0.22)
        if stack and k > 0.02:
            if shadows:
                soft_shadow(box, cx + 1, cy + 13, 23 * min(1.1, k), 8 * min(1.1, k), 0.95)
            thing(box, stack[0], cx, cy, k, scale)
    for i, (stack, k, moved) in enumerate(states):
        cx, cy = at(i)
        if stack:
            # counts stay at the font's own pixel size, in the item's lower right corner
            edge = 8 * (scale - 1)
            count(box, stack[1], cx + edge * k, cy + edge * k, k)
        if moved:
            puff(box, t, i % cols, i // cols, cx, cy, scale)


# ---- the pointer, the tool of these scenes: a pixel arrow one pixel thick over its shadow ----
ARROW = ["X.........", "XX........", "XwX.......", "XwwX......", "XwwwX.....", "XwwwwX....", "XwwwwwX...", "XwwwwwwX..",
         "XwwwwwwwX.", "XwwwwwXXXX", "XwwXwwX...", "XwX.XwwX..", "XX..XwwX..", "X....XwwX.", ".....XXX.."]


def cursor(box, t, home, target):
    go = smooth((t - 180) / 500)
    k = go * (1 - smooth((t - 1600) / 600))
    x = home[0] + (target[0] - home[0]) * k
    y = home[1] + (target[1] - home[1]) * k - math.sin(math.pi * go) * 5 * (t < CLICK)
    down = clamp(1 - abs(t - CLICK - 20) / 90) if t < CLICK + 110 else 0
    face = np.zeros((len(ARROW), len(ARROW[0]), 4), np.float32)
    for v, line in enumerate(ARROW):
        for u, c in enumerate(line):
            if c != ".":
                face[v, u] = (0.10, 0.10, 0.11, 1) if c == "X" else (0.97, 0.96, 0.95, 1)
    dark = face.copy()
    dark[..., :3] = 0
    box.quad(dark, (x + 3 - down, y + 3 - down), (1, 0), (0, 1), alpha=0.35)
    side = face.copy()
    side[..., :3] *= 0.45
    x, y = x + down, y + down
    if down < 0.5:
        box.quad(side, (x + 1, y + 1), (1, 0), (0, 1))
    box.quad(face, (x, y), (1, 0), (0, 1))


# ---- A: the chest window as the game draws it ----
def scene_a(box, t):
    mess, tidy = inventory(9, 3, STACKS, 11)
    panel = tex(MC / "gui/container/generic_54.png")
    px, py = 11, 28
    soft_shadow(box, W / 2 + 2, py + 70, 250, 16, 0.95)
    for sv, dv, hh in ((0, 0, 7), (17, 7, 54), (215, 61, 7)):
        box.sprite(panel, px, py + dv, src=(0, sv, 176, hh))
    contents(box, t, 9, 3, mess, tidy, lambda i: (px + 16 + i % 9 * 18, py + 16 + i // 9 * 18))
    cursor(box, t, (178, 104), (px + 8 + 6 * 18 + 6, py + 8 + 2 * 18 + 6))


# ---- B: no window, the game's slots as separate thick tiles ----
def scene_b(box, t):
    mess, tidy = inventory(9, 3, STACKS, 11)
    slot = tex(MC / "gui/sprites/container/slot.png")
    px, py = 10, 31
    for i in range(27):
        x, y = px + i % 9 * 20, py + i // 9 * 22
        soft_shadow(box, x + 10, y + 21, 24, 6, 0.9)
        box.rect(x, y + 18, 18, 2, (50, 46, 56))
        box.sprite(slot, x, y)
    contents(box, t, 9, 3, mess, tidy, lambda i: (px + 9 + i % 9 * 20, py + 9 + i // 9 * 22))
    cursor(box, t, (182, 108), (px + 6 * 20 + 11, py + 2 * 22 + 11))


# ---- C: no slots at all: big items on the dark, each over its own contact shadow ----
def scene_c(box, t):
    stacks = [s for s in STACKS[:15] if s[0] != "diamond"]
    mess, tidy = inventory(6, 3, stacks, 5)
    px, py = 22, 30
    contents(box, t, 6, 3, mess, tidy, lambda i: (px + i % 6 * 31, py + i // 6 * 33), 1.5, True)
    cursor(box, t, (182, 110), (px + 3 * 31 + 3, py + 2 * 33 + 2))


animate(MOD / "dev/preview/out", (("A   Chest window", scene_a), ("B   Slot tiles", scene_b), ("C   Loose items", scene_c)),
        PERIOD, (500, 830, 880, 940, 1800), key="MIDDLE BUTTON", pressed=lambda t: CLICK <= t < CLICK + 200,
        caption="Sort order: Creative")
