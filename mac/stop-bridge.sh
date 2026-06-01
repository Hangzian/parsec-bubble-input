#!/usr/bin/env zsh
set -euo pipefail

APP_DIR="${PBI_APP_DIR:-$HOME/Library/Application Support/ParsecBubbleInput}"

if [[ -f "$APP_DIR/bridge.env" ]]; then
  set -a
  source "$APP_DIR/bridge.env"
  set +a
fi

PORT="${PBI_PORT:-8765}"

if [[ -f "$APP_DIR/bridge.pid" ]]; then
  kill "$(cat "$APP_DIR/bridge.pid")" >/dev/null 2>&1 || true
  rm -f "$APP_DIR/bridge.pid"
fi

for pid in ${(f)"$(lsof -tiTCP:"$PORT" -sTCP:LISTEN 2>/dev/null || true)"}; do
  [[ -n "$pid" ]] && kill "$pid" >/dev/null 2>&1 || true
done
