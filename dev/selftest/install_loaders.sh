#!/usr/bin/env bash
# Install the loaders the self-test launches: Forge and NeoForge with their official installers into .work/loaders/mcroot,
# Quilt as its launch profile from meta.quiltmc.org plus its libraries in .work/loaders/quilt.
# Usage: install_loaders.sh [forge] [neoforge] [quilt]   (default: all)
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
META=${MODRINTH_META:-$HOME/.local/share/ModrinthApp/meta}
JAVA=$(ls -d "$META"/java_versions/zulu25*/bin | head -1)/java
D="$HERE/.work/loaders"
WANT=" ${*:-forge neoforge quilt} "
QUILT=0.31.0-beta.4
mkdir -p "$D/mcroot"
[ -f "$D/mcroot/launcher_profiles.json" ] || echo '{"profiles":{}}' > "$D/mcroot/launcher_profiles.json"
for url in \
    https://maven.minecraftforge.net/net/minecraftforge/forge/26.2-65.1.3/forge-26.2-65.1.3-installer.jar \
    https://maven.minecraftforge.net/net/minecraftforge/forge/26.3-66.0.3/forge-26.3-66.0.3-installer.jar \
    https://maven.neoforged.net/releases/net/neoforged/neoforge/26.2.0.88/neoforge-26.2.0.88-installer.jar \
    https://maven.neoforged.net/releases/net/neoforged/neoforge/26.3.0.16-beta/neoforge-26.3.0.16-beta-installer.jar \
    https://maven.minecraftforge.net/net/minecraftforge/forge/1.21.1-52.1.16/forge-1.21.1-52.1.16-installer.jar \
    https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-installer.jar \
    https://maven.minecraftforge.net/net/minecraftforge/forge/1.21.4-54.1.18/forge-1.21.4-54.1.18-installer.jar \
    https://maven.minecraftforge.net/net/minecraftforge/forge/1.21.8-58.1.22/forge-1.21.8-58.1.22-installer.jar \
    https://maven.minecraftforge.net/net/minecraftforge/forge/1.21.11-61.2.1/forge-1.21.11-61.2.1-installer.jar \
    https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.252/neoforge-21.1.252-installer.jar \
    https://maven.neoforged.net/releases/net/neoforged/neoforge/21.4.158/neoforge-21.4.158-installer.jar \
    https://maven.neoforged.net/releases/net/neoforged/neoforge/21.8.54/neoforge-21.8.54-installer.jar \
    https://maven.neoforged.net/releases/net/neoforged/neoforge/21.11.45/neoforge-21.11.45-installer.jar; do
    [[ "$WANT" == *" $(basename "$url" | cut -d- -f1) "* ]] || continue
    jar="$D/$(basename "$url")"
    [ -f "$jar" ] || curl -sfL -o "$jar" "$url"
    J="$JAVA"; case "$(basename "$jar")" in forge-1.2[01]*) J=/usr/lib/jvm/java-17-openjdk/bin/java;; esac  # Forge 1.21.x installers only produce the expected client jar on Java 17
    (cd "$D" && "$J" -jar "$jar" --installClient mcroot > "$jar.log" 2>&1) && echo "installed $(basename "$url")"
done
if [[ "$WANT" == *" quilt "* ]]; then
    mkdir -p "$D/quilt"
    for ver in 26.2 26.3 1.20.1 1.21.1 1.21.4 1.21.8 1.21.11; do
        json="$D/quilt/$ver.json"
        [ -f "$json" ] || curl -sfL -o "$json" "https://meta.quiltmc.org/v3/versions/loader/$ver/$QUILT/profile/json"
        python3 - "$json" <<'PY' | while read -r url rel; do
import json, sys
for lib in json.load(open(sys.argv[1]))["libraries"]:
    g, a, v = lib["name"].split(":")[:3]
    rel = f"{g.replace('.', '/')}/{a}/{v}/{a}-{v}.jar"
    print(lib["url"].rstrip("/") + "/" + rel, rel)
PY
            dst="$D/quilt/libraries/$rel"
            [ -f "$dst" ] || curl -sfL --create-dirs -o "$dst" "$url"
        done
        echo "installed quilt-loader $QUILT for $ver"
    done
fi
