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

# 1. Fetch latest refs
log "[$ENV] Fetching latest refs"
git -C "$REPO" fetch origin

# 2. Resolve target SHA
if [[ "$ENV" == "button" ]]; then
    SHA=$(git -C "$REPO" rev-parse origin/main)
else
    SHA=$(git -C "$REPO" for-each-ref \
        --sort=-committerdate \
        --format='%(refname:short) %(objectname)' \
        'refs/remotes/origin/*' | \
        grep -v 'origin/HEAD' | \
        awk 'NR==1{print $2}')
fi
log "[$ENV] Target SHA: $SHA"

# 3. Exit if already deployed
DEPLOYED_FILE=~/deployed_commit
if [[ -f "$DEPLOYED_FILE" ]] && [[ "$(cat "$DEPLOYED_FILE")" == "$SHA" ]]; then
    log "[$ENV] SHA $SHA already deployed, nothing to do"
    exit 0
fi

# 4. Exit if CI status is not success
STATUS=$(curl -s "https://api.github.com/repos/zwalsh/button/commits/$SHA/status" \
    -H "Authorization: token $TOKEN" | jq -r '.state // empty') || true
log "[$ENV] CI status for $SHA: ${STATUS:-<empty>}"
if [[ "$STATUS" != "success" ]]; then
    log "[$ENV] CI not green (state=$STATUS), skipping deploy"
    exit 0
fi

log "[$ENV] Starting deploy of $SHA"

on_error() {
    local exit_code=$?
    local line=$1
    log "[$ENV] DEPLOY FAILED at line $line (exit code $exit_code)"
}
trap 'on_error $LINENO' ERR

# 5. Check out the target commit
log "[$ENV] Checking out $SHA"
if [[ "$ENV" == "button" ]]; then
    git -C "$REPO" checkout -f main
else
    git -C "$REPO" -c advice.detachedHead=false checkout -f "$SHA"
fi

# 6. Build
log "[$ENV] Building"
"$REPO/gradlew" -p "$REPO" assemble

# 7. Unpack into release directory
RELEASE_DIR=~/releases/$SHA
log "[$ENV] Unpacking to $RELEASE_DIR"
mkdir -p "$RELEASE_DIR"
tar -xf "$REPO/build/distributions/button.tar" -C "$RELEASE_DIR"

# 8. Run database migrations (testbutton only migrates for commits on main)
if [[ "$ENV" == "button" ]] || git -C "$REPO" merge-base --is-ancestor "$SHA" origin/main; then
    log "[$ENV] Running database migrations"
    "$REPO/db/migrate.sh"
else
    log "[$ENV] Skipping database migrations: $SHA is not on main"
fi

# 9. Atomically update the current symlink
log "[$ENV] Updating current symlink to $RELEASE_DIR"
ln -sfn "$RELEASE_DIR" ~/releases/current

# 10. Restart the service
log "[$ENV] Restarting $ENV service"
sudo systemctl restart "$ENV"

# 11. Record the deployed commit
echo "$SHA" > "$DEPLOYED_FILE"
log "[$ENV] Deploy of $SHA complete"

# 12. Prune old releases, keeping the 3 most recent
log "[$ENV] Pruning old releases"
ls -t ~/releases/ | grep -v '^current$' | tail -n +4 | xargs -I{} rm -rf ~/releases/{}
