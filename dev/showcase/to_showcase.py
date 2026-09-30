#!/usr/bin/env python3
"""Showcase frames -> Showcase/out/tp_<scene>/: tp_<scene>.mp4 (60 fps, full res) and tp_<scene>-50fps.mp4 (GIF source,
downscaled 1/3 so one GUI pixel at gui scale 3 is one GIF pixel).
to_showcase.py <showcase-frames-dir> <Showcase-dir>"""
import subprocess, sys
from pathlib import Path
from PIL import Image
sys.path.insert(0, str(Path(__file__).parent))
from make_clips import frames, load


def encode(fr, fps, out, shrink):
    t, i, n = fr[0][0], 0, 0
    w, h = fr[0][1], fr[0][2]
    W, H = (w // 3, h // 3) if shrink else (w // 2 * 2, h // 2 * 2)
    args = ["ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(fps),
            "-i", "-", "-c:v", "libx264", "-pix_fmt", "yuv444p" if shrink else "yuv420p", "-crf", "0" if shrink else "16",
            "-movflags", "+faststart", str(out)]
    p = subprocess.Popen(args, stdin=subprocess.PIPE)
    while t <= fr[-1][0]:
        while i + 1 < len(fr) and fr[i + 1][0] <= t:
            i += 1
        im = load(fr[i][3], fr[i][1], fr[i][2])
        im = im.crop((0, 0, W * 3, H * 3)).resize((W, H), Image.BOX) if shrink else im.crop((0, 0, W, H))
        p.stdin.write(im.tobytes())
        n += 1
        t += 1000 / fps
    p.stdin.close()
    p.wait()
    return n


src, root = Path(sys.argv[1]), Path(sys.argv[2])
for d in sorted(src.iterdir()):
    fr = frames(d)
    out = root / "out" / f"tp_{d.name}"
    out.mkdir(parents=True, exist_ok=True)
    a = encode(fr, 60, out / f"tp_{d.name}.mp4", False)
    b = encode(fr, 50, out / f"tp_{d.name}-50fps.mp4", True)
    print(f"{d.name:15s} {a} frames @60, {b} @50, {fr[0][1]}x{fr[0][2]}")
