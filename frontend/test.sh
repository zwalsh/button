#!/usr/bin/env bash

# Source nvm
. ~/.nvm/nvm.sh

npm ci --prefix frontend --cache ~/.npm --no-audit --no-fund
npm --prefix frontend test
