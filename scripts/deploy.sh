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
    # testbutton: source-based deploy (temporary — will move to test-deploy.sh)

    # 1. Fetch latest refs
    log "[testbutton] Fetching latest refs"
    git -C "$REPO" fetch origin

    # 2. Resolve target SHA (tip of most-recently-updated remote branch)
    SHA=$(git -C "$REPO" for-each-ref \
        --sort=-committerdate \
        --format='%(refname:short) %(objectname)' \
        'refs/remotes/origin/*' | \
        grep -v 'origin/HEAD' | \
        awk 'NR==1{print $2}')
    log "[testbutton] Target SHA: $SHA"

    # 3. Exit if already deployed
    if [[ -f "$DEPLOYED_FILE" ]] && [[ "$(cat "$DEPLOYED_FILE")" == "$SHA" ]]; then
        log "[testbutton] SHA $SHA already deployed, nothing to do"
        exit 0
    fi

    # 4. Exit if CI status is not success
    STATUS=$(curl -s "https://api.github.com/repos/zwalsh/button/commits/$SHA/status" \
        -H "Authorization: token $TOKEN" | jq -r '.state // empty') || true
    log "[testbutton] CI status for $SHA: ${STATUS:-<empty>}"
    if [[ "$STATUS" != "success" ]]; then
        log "[testbutton] CI not green (state=$STATUS), skipping deploy"
        exit 0
    fi

    log "[testbutton] Starting deploy of $SHA"

    # 5. Check out the target commit
    log "[testbutton] Checking out $SHA"
    git -C "$REPO" -c advice.detachedHead=false checkout -f "$SHA"

    # 6. Build
    log "[testbutton] Building"
    "$REPO/gradlew" -p "$REPO" assemble

    # 7. Unpack into release directory
    RELEASE_DIR=~/releases/$SHA
    log "[testbutton] Unpacking to $RELEASE_DIR"
    mkdir -p "$RELEASE_DIR"
    tar -xf "$REPO/build/distributions/button.tar" -C "$RELEASE_DIR"

    # 8. Run database migrations (only for commits on main)
    if git -C "$REPO" merge-base --is-ancestor "$SHA" origin/main; then
        log "[testbutton] Running database migrations"
        "$REPO/db/migrate.sh"
    else
        log "[testbutton] Skipping database migrations: $SHA is not on main"
    fi

    # 9. Atomically update the current symlink
    log "[testbutton] Updating current symlink to $RELEASE_DIR"
    ln -sfn "$RELEASE_DIR" ~/releases/current

    # 10. Restart the service
    log "[testbutton] Restarting testbutton service"
    sudo systemctl restart testbutton

    # 11. Record the deployed commit
    echo "$SHA" > "$DEPLOYED_FILE"
    log "[testbutton] Deploy of $SHA complete"

    # 12. Prune old releases, keeping the 3 most recent
    log "[testbutton] Pruning old releases"
    ls -t ~/releases/ | grep -v '^current$' | tail -n +4 | xargs -I{} rm -rf ~/releases/{}
fi
