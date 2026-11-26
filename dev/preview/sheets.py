#!/usr/bin/env python3
"""One contact sheet per settings preview from a ui run's frames, into dev/preview/out/scenes/.

usage: python3 -I dev/preview/sheets.py [<frames dir>]
"""
import re
import subprocess
import sys
from pathlib import Path

MOD = Path(__file__).resolve().parents[2]
FRAMES = MOD.parents[1] / "tools/ModTest/.work/TidyPockets/26.3-fabric-ui-1920-1080/out/frames"
TOOL = Path.home() / ".claude/skills/live-preview/scripts/frames.py"


def main():
    frames = Path(sys.argv[1]) if len(sys.argv) > 1 else FRAMES
    kinds = {}
    for p in frames.glob("ui-pv-*"):
        m = re.fullmatch(r"ui-pv-([a-z]+)(?:-([a-z0-9]+))?-(\d+)", p.name)
        if m:
            kind, variant, ms = m.groups()
            kinds.setdefault(kind, []).append((variant == "title", variant or "", int(ms), p.name[6:]))
    for kind, shots in sorted(kinds.items()):
        names = [s[3] for s in sorted(shots)]
        subprocess.run([sys.executable, "-I", str(TOOL), "sheet", str(frames), str(MOD / f"dev/preview/out/scenes/{kind}.png"), *names], check=True)


if __name__ == "__main__":
    main()
