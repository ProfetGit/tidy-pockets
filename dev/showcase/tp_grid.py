#!/usr/bin/env python3
"""The description clip: the six tpg_* takes in a 2x3 grid, 836 px wide, 60 fps -> docs/clips/showcase.webp.
Record first: media/Showcase/scenes/tidypockets_grid.py, then pack-showcase showcase.py on the tpg_*.json scenes."""
import json
import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path.home() / ".claude/skills/pack-showcase/scripts"))
import frame  # noqa: E402
import make_gif  # noqa: E402
import pixelfont  # noqa: E402

MOD = Path(__file__).resolve().parents[2]
SHOW = MOD.parents[1] / "media/Showcase"
CELL = (396, 406)
FPS, START, LENGTH, LIMIT = 60, 0.85, 4.9, 10_000_000
# ffmpeg crop of the 1920x1080 take: inventory screens are 198x203 GUI px at GUI scale 4, so they scale exactly 2:1
GUI = "crop=792:812:244:88,scale=396:406:flags=area"
WORLD = "crop=1054:1080:433:0,scale=396:406:flags=lanczos,hqdn3d=1.5:1.5:6:6"
CELLS = [("tpg_sort", GUI, "MIDDLE-CLICK TO SORT"), ("tpg_lock", GUI, "ALT-CLICK LOCKS A SLOT"),
         ("tpg_tools", GUI.replace(":244:", ":212:"), "DEPOSIT, RESTOCK, SEARCH"), ("tpg_mouse", GUI, "SHIFT-DRAG AND SCROLL"),
         ("tpg_refill", WORLD, "HOTBAR REFILLS ITSELF"), ("tpg_protect", WORLD, "WORN TOOLS SWAP OUT")]


def plate(brand: dict, text: str) -> Image.Image:
    lbl = pixelfont.label(text, lambda _: "#FFFFFF")
    lbl = lbl.resize((lbl.width * 2, lbl.height * 2), Image.NEAREST)
    p = make_gif.plate((lbl.width + 12) // 2, (lbl.height + 6) // 2, brand["bg"], make_gif.INK, brand["hi"], brand["lo"])
    p = p.resize((p.width * 2, p.height * 2), Image.NEAREST)
    p.alpha_composite(lbl, ((p.width - lbl.width) // 2, (p.height - lbl.height) // 2))
    return p


def hold(ims: list) -> list:
    """Keeps a dim pixel until it really changes: the world drifting behind a menu would re-encode every frame."""
    out, held = [], None
    for im in ims:
        a = np.asarray(im.convert("RGB"), dtype=np.int16)
        if held is None:
            held = a.copy()
        else:
            moved = (np.abs(a - held).max(axis=2) > 7) | (a.max(axis=2) > 70)
            held[moved] = a[moved]
        out.append(Image.fromarray(held.astype(np.uint8)))
    return out


def zoom(brand: dict, take: Path, shots: list, work: Path) -> None:
    """The first hotbar slot, 4 px per GUI px, under the caption: its count and wear are the whole point in-world."""
    work.mkdir()
    # slot 0 with its selector at GUI scale 3 on 1920x1080: 24x23 GUI px at (684, 1011)
    make_gif.subprocess.run(["ffmpeg", "-v", "error", "-y", "-ss", str(START), "-i", str(take), "-frames:v", str(len(shots)),
                             "-vf", "crop=72:69:684:1011", str(work / "%04d.png")], check=True)
    for shot, f in zip(shots, sorted(work.glob("*.png"))):
        slot = Image.open(f).convert("RGBA").resize((24, 23), Image.BOX).resize((96, 92), Image.NEAREST)
        box = make_gif.plate(52, 50, brand["bg"], make_gif.INK, brand["hi"], brand["lo"]).resize((104, 100), Image.NEAREST)
        box.alpha_composite(slot, (4, 4))
        shot.paste(box, (10, 38), box)


def main() -> None:
    cfg = json.loads((SHOW / "gifs.json").read_text())
    brand = make_gif.brand_parts(cfg["brands"]["tidypockets"], SHOW)
    n_frames = round(LENGTH * FPS)
    with tempfile.TemporaryDirectory(dir=SHOW / ".work") as tmp:
        cells = []
        for n, (clip, vf, text) in enumerate(CELLS):
            work = Path(tmp) / str(n)
            work.mkdir()
            make_gif.subprocess.run(["ffmpeg", "-v", "error", "-y", "-ss", str(START), "-i", str(SHOW / "out" / clip / f"{clip}.mp4"),
                                     "-frames:v", str(n_frames), "-vf", vf, str(work / "%04d.png")], check=True)
            frames = sorted(work.glob("*.png"))
            if len(frames) < n_frames:
                sys.exit(f"{clip}: only {len(frames)} frames from {START} s")
            shots = [Image.open(f).convert("RGB") for f in frames]
            if vf.startswith(GUI[:14]):
                shots = hold(shots)
            else:
                zoom(brand, SHOW / "out" / clip / f"{clip}.mp4", shots, work / "zoom")
            layer = Image.new("RGBA", CELL, (0, 0, 0, 0))
            cap = plate(brand, text)
            layer.alpha_composite(cap, ((CELL[0] - cap.width) // 2, 4))
            base, rects = frame.apply(brand, layer, [(0, 0, *CELL)])
            cells.append((base, rects[0], shots))
        w, h = cells[0][0].size
        if w * 2 != 836:
            sys.exit(f"cell is {w} px wide, expected 418")
        ims = []
        for i in range(n_frames):
            sheet = Image.new("RGBA", (w * 2, h * 3), (0, 0, 0, 0))
            for n, (base, (rx, ry, _, _), frames) in enumerate(cells):
                cell = Image.new("RGBA", base.size, (0, 0, 0, 0))
                cell.paste(frames[i].convert("RGBA"), (rx, ry))
                cell.alpha_composite(base)
                sheet.paste(cell, ((n % 2) * w, (n // 2) * h))
            ims.append(sheet)
        dur = [round((i + 1) * 1000 / FPS) - round(i * 1000 / FPS) for i in range(n_frames)]
        out = MOD / "docs/clips/showcase.webp"
        for q in (66, 62, 58, 54, 50):
            ims[0].save(out, save_all=True, append_images=ims[1:], duration=dur, loop=0, quality=q, method=4,
                        minimize_size=False, kmin=157, kmax=312)
            size = out.stat().st_size
            print(f"{n_frames} frames, {w * 2}x{h * 3}, quality {q}: {size:,} bytes", flush=True)
            if size <= LIMIT:
                break
        else:
            sys.exit(f"still over {LIMIT:,} bytes")
    print(out)


if __name__ == "__main__":
    main()
