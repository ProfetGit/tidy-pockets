#!/usr/bin/env bash
# Run the in-game self-test for one target off-screen. A thin wrapper over the shared launcher ModTest/client.py
# (headless KWin, launches the built jar the way a player's launcher does: Fabric from the ModrinthApp meta, Quilt =
# the Fabric jar plus Fabric API on Quilt Loader, NeoForge and Forge from their official installers,
# dev/selftest/install_loaders.sh; machine-wide client slots; mixin audit). For the test tiers use
# `python3 ModTest/mt.py TidyPockets quick|full|release`.
# Usage: dev/selftest/run.sh <mc-version> <fabric|quilt|neoforge|forge> [boot|full|remote|compat|showcase] [extra-mods-dir]
# Env: SKIP_BUILD=1, MAX_FPS (default 60), SKIN_PACK=<zip>, PLAYER_UUID.
# Results: dev/selftest/.work/<target>/out/results.json (+ frames/), logs in .work/<target>/game. Exit code 1 on any failure.
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
mkdir -p "$WORK"

BUILD="$TARGET"
[ "$LOADER" = quilt ] && BUILD="$VER-fabric"
[ -n "${SKIP_BUILD:-}" ] || (cd "$ROOT" && ./gradlew --console=plain -q ":$BUILD:build" -x test)
ARGS=(--jar "$(ls "$ROOT"/versions/"$BUILD"/build/libs/tidypockets-*.jar | head -1)")
[ "$LOADER" = fabric ] || [ "$LOADER" = quilt ] && ARGS+=(--fabric-api)
if [ -n "$EXTRA" ]; then for j in "$EXTRA"/*.jar; do ARGS+=(--mod "$j"); done; fi
[ -n "${SKIN_PACK:-}" ] && ARGS+=(--pack "$SKIN_PACK")
GUI_SCALE=2
[ "$MODE" = showcase ] && GUI_SCALE=3

if [ "$MODE" = remote ]; then
    ARGS+=(--join 127.0.0.1:25599)
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

set +e
python3 "$ROOT/../ModTest/client.py" "$VER" "$LOADER" "$OUT" --game "$WORK/game" "${ARGS[@]}" \
    --user PocketTester --uuid "${PLAYER_UUID:-5e1dcaa0-0000-4000-8000-000000000002}" --width 1280 --height 720 --timeout 900 \
    --opt "fps=${MAX_FPS:-60}" --opt volume=0.0 --opt render_distance=4 --opt "gui_scale=$GUI_SCALE" \
    --log-errors 'ERROR\]: tidypockets\.mixins\.json' \
    -D "tidypockets.selftest=$OUT" -D "tidypockets.selftest.mode=$MODE" -D modtest.audit=1 --label "TidyPockets $TARGET $MODE"
FAIL=$?
set -e

if [ "$MODE" = remote ]; then
    echo stop >&9
    exec 9>&-
    sleep 5
    pkill -f -- "[t]ail -f $WORK/server/stdin" || true
fi
exit $FAIL
