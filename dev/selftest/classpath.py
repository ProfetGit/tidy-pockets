#!/usr/bin/env python3
"""Print the client classpath (libraries incl. linux natives) from a version JSON:
classpath.py <json> <libroot> [skip-group ...]  (e.g. skip net.fabricmc org.ow2.asm to swap the Fabric loader for Quilt)"""
import json, os, sys

spec, libroot = sys.argv[1], sys.argv[2]
skip = set(sys.argv[3:])
out = []
for lib in json.load(open(spec))["libraries"]:
    parts = lib["name"].split(":")
    g, a, v = parts[:3]
    if g in skip:
        continue
    cls = parts[3] if len(parts) > 3 else None
    if cls and cls.startswith("natives-") and cls != "natives-linux":
        continue
    rules = lib.get("rules")
    if rules and not any(r.get("action") == "allow" and r.get("os", {}).get("name") in (None, "linux") for r in rules):
        continue
    p = os.path.join(libroot, *g.split("."), a, v, f"{a}-{v}" + (f"-{cls}" if cls else "") + ".jar")
    if os.path.exists(p) and p not in out:
        out.append(p)
print(":".join(out))
