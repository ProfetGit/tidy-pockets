#!/usr/bin/env bash
# Run the in-game self-test for one target inside a headless KWin session.
# Usage: dev/selftest/run.sh <mc-version> <fabric|quilt|neoforge|forge> [boot|full] [extra-mods-dir]
# Every loader launches the built jar the way a player's launcher does: Fabric from the ModrinthApp profile, Quilt (the
# Fabric jar plus Fabric API on Quilt Loader) from Quilt's launch profile, NeoForge and Forge from an install made by
# their official installers (dev/selftest/install_loaders.sh).
# Results: dev/selftest/.work/<target>/out/results.json (+ frames/), logs next to it. Exit code 1 on any failure.
set -euo pipefail
VER=${1:?usage: run.sh <mc-version> <loader> [boot|full] [extra-mods-dir]}
LOADER=${2:?loader}
MODE=${3:-full}
EXTRA=${4:-}
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE/../.." && pwd)
TARGET="$VER-$LOADER"
WORK="$HERE/.work/$TARGET"
OUT="$WORK/out"
META=${MODRINTH_META:-$HOME/.local/share/ModrinthApp/meta}
JAVA=$(ls -d "$META"/java_versions/zulu25*/bin | head -1)/java
LOG="$WORK/client.log"

rm -rf "$OUT" && mkdir -p "$OUT" "$WORK"

options() {
    mkdir -p "$1"
    GUI_SCALE=2
    [ "$MODE" = showcase ] && GUI_SCALE=3
    cat > "$1/options.txt" <<OPT
onboardAccessibility:false
soundCategory_master:0.0
pauseOnLostFocus:false
renderDistance:4
simulationDistance:5
maxFps:60
guiScale:$GUI_SCALE
narrator:0
tutorialStep:none
skipMultiplayerWarning:true
joinedFirstServer:true
OPT
}

QUICKPLAY=""
[ "$MODE" = remote ] && QUICKPLAY="--quickPlayMultiplayer 127.0.0.1:25599"
BUILD="$TARGET"
[ "$LOADER" = quilt ] && BUILD="$VER-fabric"
[ -n "${SKIP_BUILD:-}" ] || (cd "$ROOT" && ./gradlew --console=plain -q ":$BUILD:build" -x test)

if [ "$LOADER" = fabric ] || [ "$LOADER" = quilt ]; then
    GAME="$WORK/game"
    VDIR=$(ls -d "$META"/versions/"$VER"-* | head -1)
    VJSON="$VDIR/$(basename "$VDIR").json"
    FAPI=$(find "$HOME/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api" -name "fabric-api-*+$VER.jar" | head -1)
    rm -rf "$GAME/mods" && mkdir -p "$GAME/mods" "$GAME/tmp"
    cp "$ROOT"/versions/"$BUILD"/build/libs/tidypockets-*.jar "$FAPI" "$GAME/mods/"
    options "$GAME"
    if [ "$LOADER" = fabric ]; then
        CP="$(python3 "$HERE/classpath.py" "$VJSON" "$META/libraries"):$VDIR/$(basename "$VDIR").jar"
        KNOT="\"-DFabricMcEmu= net.minecraft.client.main.Main \" -cp \"$CP\" net.fabricmc.loader.impl.launch.knot.KnotClient"
    else
        QDIR="$HERE/.work/loaders/quilt"
        [ -f "$QDIR/$VER.json" ] || { echo "FAIL quilt $VER is not installed in $QDIR (see dev/selftest/install_loaders.sh quilt)"; exit 1; }
        # the ModrinthApp profile minus its Fabric loader, Mixin and ASM, which Quilt's profile brings itself
        CP="$(python3 "$HERE/classpath.py" "$QDIR/$VER.json" "$QDIR/libraries"):$(python3 "$HERE/classpath.py" "$VJSON" "$META/libraries" net.fabricmc org.ow2.asm):$VDIR/$(basename "$VDIR").jar"
        KNOT="-cp \"$CP\" org.quiltmc.loader.impl.launch.knot.KnotClient"
    fi
    ASSET_INDEX=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['assetIndex']['id'])" "$VJSON")
    CMD="cd \"$GAME\" && exec \"$JAVA\" -Xmx3G --enable-native-access=ALL-UNNAMED \
 -Dorg.lwjgl.system.SharedLibraryExtractPath=\"$GAME/natives\" -Djava.io.tmpdir=\"$GAME/tmp\" \
 -Dtidypockets.selftest=\"$OUT\" -Dtidypockets.selftest.mode=$MODE \
 $KNOT \
 --username PocketTester --uuid 5e1dcaa0-0000-4000-8000-000000000002 --accessToken 0 --version $VER --versionType release \
 --gameDir \"$GAME\" --assetsDir \"$META/assets\" --assetIndex $ASSET_INDEX --width 1280 --height 720 $QUICKPLAY"
else
    GAME="$WORK/game"
    MCROOT="$HERE/.work/loaders/mcroot"
    if [ "$LOADER" = forge ]; then PAT="^$VER-forge-"; else PAT="^neoforge-$VER\."; fi
    VID=$(ls "$MCROOT/versions" | grep -E "$PAT" | sort -V | tail -1)
    [ -n "$VID" ] || { echo "FAIL $LOADER $VER is not installed in $MCROOT (see dev/selftest/install_loaders.sh)"; exit 1; }
    VDIR=$(ls -d "$META"/versions/"$VER"-* | head -1)
    rm -rf "$GAME/mods" && mkdir -p "$GAME/mods" "$GAME/tmp"
    cp "$ROOT"/versions/"$TARGET"/build/libs/tidypockets-*.jar "$GAME/mods/"
    options "$GAME"
    ARGS=$(python3 "$HERE/launch.py" "$MCROOT" "$VID" "$VDIR/$(basename "$VDIR").json" "$META/libraries" "$META/assets" "$GAME" \
        "-Djava.io.tmpdir=$GAME/tmp" "-Dtidypockets.selftest=$OUT" "-Dtidypockets.selftest.mode=$MODE")
    CMD="cd \"$GAME\" && exec \"$JAVA\" $ARGS $QUICKPLAY"
fi
[ -n "$EXTRA" ] && cp "$EXTRA"/*.jar "$GAME/mods/"

# 26.2 uses GLFW, which only behaves under XWayland here (native Wayland pointer warps kill the connection)
UNSET=""
[ "$VER" = 26.2 ] && UNSET="unset WAYLAND_DISPLAY"
cat > "$WORK/launch.sh" <<LAUNCH
#!/usr/bin/env bash
$UNSET
$CMD > "$LOG" 2>&1
LAUNCH
chmod +x "$WORK/launch.sh"

if [ "$MODE" = remote ]; then
    SRV="$WORK/server"
    rm -rf "$SRV" && mkdir -p "$SRV"
    echo "eula=true" > "$SRV/eula.txt"
    cat > "$SRV/server.properties" <<'PROPS'
online-mode=false
server-ip=127.0.0.1
server-port=25599
level-type=minecraft\:flat
generate-structures=false
spawn-protection=0
gamemode=survival
difficulty=peaceful
view-distance=4
simulation-distance=4
PROPS
    python3 -c "import uuid,hashlib,json,sys; h=bytearray(hashlib.md5(b'OfflinePlayer:PocketTester').digest()); h[6]=h[6]&0x0f|0x30; h[8]=h[8]&0x3f|0x80; print(json.dumps([{'uuid':str(uuid.UUID(bytes=bytes(h))),'name':'PocketTester','level':4,'bypassesPlayerLimit':False}]))" > "$SRV/ops.json"
    VDIR=$(ls -d "$META"/versions/"$VER"-* | head -1)
    SCP="$(python3 "$HERE/classpath.py" "$VDIR/$(basename "$VDIR").json" "$META/libraries"):$VDIR/$(basename "$VDIR").jar"
    mkfifo "$SRV/stdin"
    (cd "$SRV" && tail -f "$SRV/stdin" | "$JAVA" -Xmx2G -cp "$SCP" net.minecraft.server.Main --nogui > "$SRV/server.log" 2>&1) &
    exec 9> "$SRV/stdin"
    for i in $(seq 1 120); do grep -q 'Done (' "$SRV/server.log" 2>/dev/null && break; sleep 1; done
    grep -q 'Done (' "$SRV/server.log" || { echo "FAIL dedicated server did not start"; tail -5 "$SRV/server.log"; exit 1; }
fi

# kwin splits the session command on spaces, so start it from a path without any
STUB="${XDG_RUNTIME_DIR:-/tmp}/tp-selftest-$$.sh"
printf '#!/usr/bin/env bash\nexec "%s"\n' "$WORK/launch.sh" > "$STUB"
chmod +x "$STUB"
XW=""
[ "$VER" = 26.2 ] && XW="--xwayland"
set +e
env -u WAYLAND_DISPLAY -u DISPLAY timeout 900 dbus-run-session -- \
  kwin_wayland --virtual --no-lockscreen $XW --width 1280 --height 720 --socket tp-$$ \
  --exit-with-session "$STUB" > "$WORK/kwin.log" 2>&1
STATUS=$?
set -e
rm -f "$STUB"

if [ "$MODE" = remote ]; then
    echo stop >&9
    exec 9>&-
    sleep 5
    pkill -f -- "[t]ail -f $WORK/server/stdin" || true
fi

FAIL=0
if [ -f "$OUT/results.json" ]; then
    python3 - "$OUT/results.json" <<'PY' || FAIL=1
import json, sys
d = json.load(open(sys.argv[1]))
bad = 0
for r in d["results"]:
    print(("PASS " if r["pass"] else "FAIL ") + r["name"] + ("" if r["pass"] else "  " + r["detail"]))
    bad += not r["pass"]
print(f"{d['loader']} {d['mc']}: {len(d['results']) - bad}/{len(d['results'])} passed")
sys.exit(1 if bad else 0)
PY
else
    echo "FAIL no results.json (session exit $STATUS); see $LOG"
    FAIL=1
fi
if grep -E 'MixinApplyError|InvalidInjectionException|InjectionError|Mixin apply .* failed|MixinTransformerError|Exception in thread' "$LOG" | head -5 | grep -q .; then
    echo "FAIL errors in log:"
    grep -E 'MixinApplyError|InvalidInjectionException|InjectionError|Mixin apply .* failed|MixinTransformerError|Exception in thread' "$LOG" | cut -c1-240 | head -5
    FAIL=1
fi
WARN=$(grep -ciE '\[[^]]*/(WARN|ERROR)\] \[(Tidy Pockets|mixin)' "$LOG" || true)
echo "log: $LOG ($WARN tidypockets/mixin warnings)"
exit $FAIL
