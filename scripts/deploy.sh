#!/bin/bash
set -euo pipefail

log() {
    echo "[$(date -u '+%Y-%m-%dT%H:%M:%SZ')] $*"
}

ENV=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --env) ENV="$2"; shift 2 ;;
        *) echo "Unknown argument: $1" >&2; exit 1 ;;
    esac
done

if [[ "$ENV" != "button" && "$ENV" != "testbutton" ]]; then
    echo "Usage: $0 --env <button|testbutton>" >&2
    exit 1
fi

REPO=~/button
TOKEN=$(cat ~/.github_token)
DEPLOYED_FILE=~/deployed_commit

on_error() {
    local exit_code=$?
    local line=$1
    log "[$ENV] DEPLOY FAILED at line $line (exit code $exit_code)"
}
trap 'on_error $LINENO' ERR

if [[ "$ENV" == "button" ]]; then
    # 1. Get latest release tag from GitHub
    log "[button] Fetching latest release"
    TAG=$(GITHUB_TOKEN="$TOKEN" gh release view --latest \
        --repo zwalsh/button \
        --json tagName \
        --jq '.tagName' 2>/dev/null) || { log "[button] No release found"; exit 0; }
    SHA="${TAG#sha-}"
    log "[button] Latest release: $TAG (SHA: $SHA)"

    # 2. Exit if already deployed
    if [[ -f "$DEPLOYED_FILE" ]] && [[ "$(cat "$DEPLOYED_FILE")" == "$TAG" ]]; then
        log "[button] $TAG already deployed, nothing to do"
        exit 0
    fi

    # 3. Exit if CI status is not success
    STATUS=$(curl -s "https://api.github.com/repos/zwalsh/button/commits/$SHA/status" \
        -H "Authorization: token $TOKEN" | jq -r '.state // empty') || true
    log "[button] CI status for $SHA: ${STATUS:-<empty>}"
    if [[ "$STATUS" != "success" ]]; then
        log "[button] CI not green (state=$STATUS), skipping deploy"
        exit 0
    fi

    log "[button] Starting deploy of $TAG"

    # 4. Download release asset
    log "[button] Downloading release asset"
    GITHUB_TOKEN="$TOKEN" gh release download "$TAG" \
        --repo zwalsh/button \
        --pattern 'button.tar' \
        --dir /tmp/ \
        --clobber

    # 5. Unpack into release directory
    RELEASE_DIR=~/releases/$TAG
    log "[button] Unpacking to $RELEASE_DIR"
    mkdir -p "$RELEASE_DIR"
    tar -xf /tmp/button.tar -C "$RELEASE_DIR"

    # 6. Update repo to release SHA so migration files are current
    log "[button] Checking out $SHA for migrations"
    git -C "$REPO" fetch origin
    git -C "$REPO" checkout -f "$SHA"

    # 7. Run database migrations
    log "[button] Running database migrations"
    "$REPO/db/migrate.sh"

    # 8. Atomically update the current symlink
    log "[button] Updating current symlink to $RELEASE_DIR"
    ln -sfn "$RELEASE_DIR" ~/releases/current

    # 9. Restart the service
    log "[button] Restarting button service"
    sudo systemctl restart button

    # 10. Record the deployed tag
    echo "$TAG" > "$DEPLOYED_FILE"
    log "[button] Deploy of $TAG complete"

    # 11. Prune old releases, keeping the 3 most recent
    log "[button] Pruning old releases"
    ls -t ~/releases/ | grep -v '^current$' | tail -n +4 | xargs -I{} rm -rf ~/releases/{}

else
    log "[testbutton] testbutton deploys are not handled here; use test-deploy.sh"
fi
