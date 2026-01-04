#!/usr/bin/env bash
set -euo pipefail

# Source nvm
. ~/.nvm/nvm.sh

npm ci --prefix frontend --cache ~/.npm --no-audit --no-fund
npm --prefix frontend test
