#!/usr/bin/env python3
"""Turn recorded showcase frames into gallery GIFs (and MP4s).

make_clips.py <showcase-frames-dir> <out-dir>
Frames are the raw RGBA files the in-game director writes (16-byte header: width, height, ms, full width).
Each scene is resampled to a steady frame rate by timestamp, then encoded with ffmpeg's two-pass palette.
"""
import shutil
import struct
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image

# scene: (fps, scale, start-ms trim, end-ms trim)
SCENES = {
    "sort": (30, 1.0, 0, 0),
    "moves": (30, 1.0, 0, 0),
    "drag": (30, 1.0, 0, 0),
    "locks": (30, 1.0, 0, 0),
    "tools": (30, 1.0, 0, 0),
    "hotbar": (30, 1.0, 0, 0),
    "open": (25, 2 / 3, 0, 0),
    "refill": (25, 2 / 3, 0, 0),
    "protect": (25, 2 / 3, 0, 0),
}
GALLERY_LIMIT = 5 * 1024 * 1024


def frames(scene_dir):
    out = []
    for f in sorted(scene_dir.glob("*.raw")):
        data = f.read_bytes()
        w, h, ms, _ = struct.unpack(">iiii", data[:16])
        out.append((ms, w, h, f))
    return out


def load(f, w, h):
    data = f.read_bytes()[16:]
    return Image.frombytes("RGBA", (w, h), data).convert("RGB")


def build(scene_dir, out_dir, fps, scale, trim_start, trim_end):
    fr = frames(scene_dir)
    if not fr:
        return None
    end = fr[-1][0] - trim_end
    tmp = Path(tempfile.mkdtemp(prefix="tp-clip-"))
    n, t, i = 0, max(trim_start, fr[0][0]), 0
    step = 1000 / fps
    while t <= end:
        while i + 1 < len(fr) and fr[i + 1][0] <= t:
            i += 1
        ms, w, h, f = fr[i]
        im = load(f, w, h)
        if scale != 1:
            im = im.resize((round(w * scale) // 2 * 2, round(h * scale) // 2 * 2), Image.LANCZOS)
        im.save(tmp / f"{n:05d}.png")
        n += 1
        t += step
    name = scene_dir.name
    gif, mp4 = out_dir / f"{name}.gif", out_dir / f"{name}.mp4"
    pal = tmp / "palette.png"
    src = ["-framerate", str(fps), "-i", str(tmp / "%05d.png")]
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", *src, "-vf", "palettegen=max_colors=192:stats_mode=diff", str(pal)], check=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", *src, "-i", str(pal), "-lavfi",
                    "paletteuse=dither=none:diff_mode=rectangle", "-loop", "0", str(gif)], check=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", *src, "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "18",
                    "-movflags", "+faststart", str(mp4)], check=True)
    Image.open(tmp / "00000.png").save(out_dir / f"{name}-first.png")
    Image.open(tmp / f"{n - 1:05d}.png").save(out_dir / f"{name}-last.png")
    shutil.rmtree(tmp)
    return gif, n


def main():
    src, out = Path(sys.argv[1]), Path(sys.argv[2])
    out.mkdir(parents=True, exist_ok=True)
    for name, (fps, scale, a, b) in SCENES.items():
        d = src / name
        if not d.is_dir():
            continue
        gif, n = build(d, out, fps, scale, a, b)
        size = gif.stat().st_size
        flag = "" if size <= GALLERY_LIMIT else "  OVER the 5 MiB gallery limit"
        w, h = Image.open(gif).size
        print(f"{name:8s} {n:4d} frames {w}x{h} {size / 1024:7.0f} KiB{flag}")


if __name__ == "__main__":
    main()
