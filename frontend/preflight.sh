#!/usr/bin/env bash
set -euo pipefail

fail() { echo "$1"; exit 1; }

# Source nvm
. ~/.nvm/nvm.sh

echo "Node: $(node -v)"
echo "npm: $(npm -v)"

NODE_MAJOR=$(node -v | sed 's/^v\([0-9]*\)\..*/\1/')
[ "$NODE_MAJOR" -ge 24 ] || fail "Node 24+ required (found $(node -v))"

[ -f frontend/package-lock.json ] || fail "frontend/package-lock.json missing"

echo "Preflight OK"
