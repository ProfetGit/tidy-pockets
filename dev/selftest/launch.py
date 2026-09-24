#!/usr/bin/env python3
"""Print a shell command that starts an installed Forge/NeoForge client the way a launcher would.

launch.py <mcroot> <version-id> <vanilla-json> <meta-libraries> <assets-dir> <game-dir> [extra jvm args...]
mcroot holds what the loader installer wrote (versions/<id>/<id>.json, libraries/, versions/<mc>/<mc>.jar).
The vanilla libraries and arguments come from a ModrinthApp version JSON, with its Fabric parts dropped.
"""
import json
import os
import shlex
import sys

mcroot, vid, vanilla_json, meta_libs, assets, game = sys.argv[1:7]
extra_jvm = sys.argv[7:]
child = json.load(open(f"{mcroot}/versions/{vid}/{vid}.json"))
parent = json.load(open(vanilla_json))
mc = child["inheritsFrom"]


def allowed(rules, features=frozenset({"has_custom_resolution"})):
    if not rules:
        return True
    ok = False
    for r in rules:
        os_rule = r.get("os", {})
        if os_rule.get("name") not in (None, "linux") or os_rule.get("arch") not in (None, "x86_64", "amd64"):
            continue
        feats = r.get("features", {})
        if any(bool(v) != (k in features) for k, v in feats.items() if v is not None):
            continue
        ok = r["action"] == "allow"
    return ok


def lib_path(lib):
    art = lib.get("downloads", {}).get("artifact")
    if art and art.get("path"):
        rel = art["path"]
    else:
        parts = lib["name"].split(":")
        g, a, v = parts[:3]
        cls = f"-{parts[3]}" if len(parts) > 3 else ""
        ext = "jar"
        if "@" in v:
            v, ext = v.split("@")
        rel = f"{g.replace('.', '/')}/{a}/{v}/{a}-{v}{cls}.{ext}"
    for root in (f"{mcroot}/libraries", meta_libs):
        p = os.path.join(root, rel)
        if os.path.exists(p):
            return p
    return None


def ident(lib):
    parts = lib["name"].split(":")
    return (parts[0], parts[1], parts[3] if len(parts) > 3 else "")


cp, seen = [], set()
libs = [(True, lib) for lib in child.get("libraries", [])] + [(False, lib) for lib in parent["libraries"]]
for from_loader, lib in libs:
    parts = lib["name"].split(":")
    # drop the ModrinthApp profile's Fabric loader and its ASM; NeoForge's own Mixin is net.fabricmc:sponge-mixin
    if not from_loader and parts[0] in ("net.fabricmc", "org.ow2.asm"):
        continue
    if len(parts) > 3 and parts[3].startswith("natives-") and parts[3] != "natives-linux":
        continue
    if not allowed(lib.get("rules")) or ident(lib) in seen:
        continue
    p = lib_path(lib)
    if p is None:
        if from_loader:
            sys.exit(f"missing library {lib['name']}")
        continue
    seen.add(ident(lib))
    cp.append(p)
cp.append(f"{mcroot}/versions/{mc}/{mc}.jar")

subs = {
    "library_directory": f"{mcroot}/libraries", "classpath_separator": ":", "version_name": vid,
    "natives_directory": f"{game}/natives", "launcher_name": "tidypockets-selftest", "launcher_version": "1",
    "classpath": ":".join(cp), "game_directory": game, "assets_root": assets,
    "assets_index_name": parent["assetIndex"]["id"], "auth_player_name": "PocketTester",
    "auth_uuid": "5e1dcaa0-0000-4000-8000-000000000003", "auth_access_token": "0", "clientid": "0",
    "auth_xuid": "0", "version_type": "release", "resolution_width": "1280", "resolution_height": "720",
    "user_type": "legacy",
}


def expand(args):
    out = []
    for a in args:
        if isinstance(a, dict):
            if not allowed(a.get("rules")):
                continue
            vals = a["value"] if isinstance(a["value"], list) else [a["value"]]
        else:
            vals = [a]
        for v in vals:
            for k, s in subs.items():
                v = v.replace("${" + k + "}", s)
            if "${" in v:
                continue
            out.append(v)
    return out


jvm = [a for a in expand(parent["arguments"]["jvm"]) if not a.startswith("-DFabricMcEmu")]
jvm += expand(child.get("arguments", {}).get("jvm", []))
game_args = expand(parent["arguments"]["game"]) + expand(child.get("arguments", {}).get("game", []))
cmd = ["-Xmx3G"] + jvm + extra_jvm + [child["mainClass"]] + game_args
print(" ".join(shlex.quote(c) for c in cmd))
