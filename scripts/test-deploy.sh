#!/bin/bash
set -euo pipefail

log() {
    echo "[$(date -u '+%Y-%m-%dT%H:%M:%SZ')] $*"
}

REPO="${BUTTON_REPO:-$HOME/button}"
TOKEN=$(cat ~/.github_token)
DEPLOYED_FILE=~/deployed_commit

on_error() {
    local exit_code=$?
    local line=$1
    log "[testbutton] *** DEPLOY FAILED *** at line $line (exit code $exit_code)"
    log "[testbutton] Check the lines above for the error output from the failing command"
}
trap 'on_error $LINENO' ERR

# 1. Find the most-recently-updated open PR labeled deploy-test
log "[testbutton] Checking for PRs labeled deploy-test"
PR_JSON=$(GITHUB_TOKEN="$TOKEN" gh pr list \
    --label deploy-test \
    --state open \
    --json number,headRefName,headRefOid,updatedAt \
    --repo zwalsh/button)

PR_COUNT=$(echo "$PR_JSON" | jq 'length')
log "[testbutton] Found $PR_COUNT open PR(s) labeled deploy-test"

SHA=$(echo "$PR_JSON" | jq -r 'sort_by(.updatedAt) | last | .headRefOid // empty')

# 2. If none found, exit
if [[ -z "$SHA" ]]; then
    log "[testbutton] No open PRs labeled deploy-test, nothing to do"
    exit 0
fi

PR_NUMBER=$(echo "$PR_JSON" | jq -r 'sort_by(.updatedAt) | last | .number')
PR_BRANCH=$(echo "$PR_JSON" | jq -r 'sort_by(.updatedAt) | last | .headRefName')
log "[testbutton] Target: PR #$PR_NUMBER ($PR_BRANCH) at SHA $SHA"

# 3. Exit if CI status is not success
STATUS=$(curl -s "https://api.github.com/repos/zwalsh/button/commits/$SHA/status" \
    -H "Authorization: token $TOKEN" | jq -r '.state // empty') || true
log "[testbutton] CI status for $SHA: ${STATUS:-<empty>}"
if [[ "$STATUS" != "success" ]]; then
    log "[testbutton] CI not green (state=$STATUS), skipping deploy"
    exit 0
fi

# 4. Exit if SHA already deployed
DEPLOYED_SHA=""
if [[ -f "$DEPLOYED_FILE" ]]; then
    DEPLOYED_SHA=$(cat "$DEPLOYED_FILE")
fi
log "[testbutton] Currently deployed: ${DEPLOYED_SHA:-<none>}"
if [[ "$DEPLOYED_SHA" == "$SHA" ]]; then
    log "[testbutton] SHA $SHA already deployed, nothing to do"
    exit 0
fi

log "[testbutton] Starting deploy of PR #$PR_NUMBER ($PR_BRANCH) at $SHA"

# 5. Check out the target commit
log "[testbutton] Fetching and checking out $SHA"
git -C "$REPO" fetch origin
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
log "[testbutton] Deploy of PR #$PR_NUMBER at $SHA complete"

# 12. Prune old releases, keeping the 3 most recent
log "[testbutton] Pruning old releases"
ls -t ~/releases/ | grep -v '^current$' | tail -n +4 | xargs -I{} rm -rf ~/releases/{}
