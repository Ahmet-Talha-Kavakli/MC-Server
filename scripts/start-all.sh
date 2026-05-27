#!/usr/bin/env bash
# Launches hub + bedwars + proxy in separate Terminal tabs (macOS)
# or in background with logs (Linux fallback).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

HUB_CMD="cd '$ROOT/network/hub' && java -Xms1G -Xmx2G -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch -jar paper.jar --nogui"
BED_CMD="cd '$ROOT/network/bedwars' && java -Xms1G -Xmx2G -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch -jar paper.jar --nogui"
PROXY_CMD="cd '$ROOT/network/proxy' && java -Xms512M -Xmx1G -XX:+UseG1GC -jar velocity.jar"

if [[ "$OSTYPE" == "darwin"* ]] && command -v osascript >/dev/null 2>&1; then
    echo "Launching three Terminal tabs (macOS)..."
    osascript <<EOF
tell application "Terminal"
    activate
    do script "$HUB_CMD"
    do script "$BED_CMD"
    do script "$PROXY_CMD"
end tell
EOF
    echo "Tabs opened. Connect to localhost:25565 once Velocity says Done!"
else
    echo "Launching in background (logs in /tmp/reinacraft-*.log)..."
    mkdir -p /tmp
    nohup bash -c "$HUB_CMD"   > /tmp/reinacraft-hub.log    2>&1 &
    nohup bash -c "$BED_CMD"   > /tmp/reinacraft-bedwars.log 2>&1 &
    sleep 5
    nohup bash -c "$PROXY_CMD" > /tmp/reinacraft-proxy.log   2>&1 &
    echo "PIDs: hub=$! (last). tail -f /tmp/reinacraft-*.log to follow."
fi
