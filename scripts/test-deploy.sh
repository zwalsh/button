#!/bin/bash
set -euo pipefail

log() {
    echo "[$(date -u '+%Y-%m-%dT%H:%M:%SZ')] $*"
}

REPO="${BUTTON_REPO:-$HOME/button}"
TOKEN=$(cat ~/.github_token)
MIGRATED_FILE=~/migrated_release
DEPLOYED_FILE=~/deployed_commit

on_error() {
    local exit_code=$?
    local line=$1
    log "[testbutton] *** DEPLOY FAILED *** at line $line (exit code $exit_code)"
    log "[testbutton] Check the lines above for the error output from the failing command"
}
trap 'on_error $LINENO' ERR

# =============================================================================
# Part 1: Run migrations from the latest GitHub release
# Keeps testbutton's schema in sync with main, independent of PR deploys.
# =============================================================================

log "[testbutton] Checking latest GitHub release"
TAG=$(GITHUB_TOKEN="$TOKEN" gh release list \
    --repo zwalsh/button \
    --json tagName,isLatest \
    --jq '.[] | select(.isLatest) | .tagName')

if [[ -z "$TAG" ]]; then
    log "[testbutton] No release found, skipping migrations"
else
    RELEASE_SHA="${TAG#sha-}"
    log "[testbutton] Latest release: $TAG (SHA: $RELEASE_SHA)"

    MIGRATED_TAG=""
    if [[ -f "$MIGRATED_FILE" ]]; then
        MIGRATED_TAG=$(cat "$MIGRATED_FILE")
    fi
    log "[testbutton] Last migrated release: ${MIGRATED_TAG:-<none>}"

    if [[ "$MIGRATED_TAG" == "$TAG" ]]; then
        log "[testbutton] Migrations already up to date"
    else
        RELEASE_STATUS=$(curl -s "https://api.github.com/repos/zwalsh/button/commits/$RELEASE_SHA/status" \
            -H "Authorization: token $TOKEN" | jq -r '.state // empty') || true
        log "[testbutton] CI status for release $TAG: ${RELEASE_STATUS:-<empty>}"

        if [[ "$RELEASE_STATUS" != "success" ]]; then
            log "[testbutton] Release CI not green, skipping migrations"
        else
            log "[testbutton] Checking out $TAG for migrations"
            git -C "$REPO" fetch origin
            git -C "$REPO" -c advice.detachedHead=false checkout -f "$RELEASE_SHA"

            log "[testbutton] Running database migrations"
            "$REPO/db/migrate.sh"

            echo "$TAG" > "$MIGRATED_FILE"
            log "[testbutton] Migrations complete for $TAG"
        fi
    fi
fi

# =============================================================================
# Part 2: Deploy the most-recently-updated open PR labeled deploy-test
# Builds from source and restarts testbutton. Runs independently of migrations.
# =============================================================================

log "[testbutton] Checking for PRs labeled deploy-test"
PR_JSON=$(GITHUB_TOKEN="$TOKEN" gh pr list \
    --label deploy-test \
    --state open \
    --json number,headRefName,headRefOid,updatedAt \
    --repo zwalsh/button)

PR_COUNT=$(echo "$PR_JSON" | jq 'length')
log "[testbutton] Found $PR_COUNT open PR(s) labeled deploy-test"

SHA=$(echo "$PR_JSON" | jq -r 'sort_by(.updatedAt) | last | .headRefOid // empty')

if [[ -z "$SHA" ]]; then
    log "[testbutton] No open PRs labeled deploy-test, nothing to deploy"
    exit 0
fi

PR_NUMBER=$(echo "$PR_JSON" | jq -r 'sort_by(.updatedAt) | last | .number')
PR_BRANCH=$(echo "$PR_JSON" | jq -r 'sort_by(.updatedAt) | last | .headRefName')
log "[testbutton] Target: PR #$PR_NUMBER ($PR_BRANCH) at SHA $SHA"

PR_STATUS=$(curl -s "https://api.github.com/repos/zwalsh/button/commits/$SHA/status" \
    -H "Authorization: token $TOKEN" | jq -r '.state // empty') || true
log "[testbutton] CI status for $SHA: ${PR_STATUS:-<empty>}"
if [[ "$PR_STATUS" != "success" ]]; then
    log "[testbutton] CI not green (state=$PR_STATUS), skipping deploy"
    exit 0
fi

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

log "[testbutton] Fetching and checking out $SHA"
git -C "$REPO" fetch origin
git -C "$REPO" -c advice.detachedHead=false checkout -f "$SHA"

log "[testbutton] Building"
"$REPO/gradlew" -p "$REPO" assemble

RELEASE_DIR=~/releases/$SHA
log "[testbutton] Unpacking to $RELEASE_DIR"
mkdir -p "$RELEASE_DIR"
tar -xf "$REPO/build/distributions/button.tar" -C "$RELEASE_DIR"

log "[testbutton] Updating current symlink to $RELEASE_DIR"
ln -sfn "$RELEASE_DIR" ~/releases/current

log "[testbutton] Restarting testbutton service"
sudo systemctl restart testbutton

echo "$SHA" > "$DEPLOYED_FILE"
log "[testbutton] Deploy of PR #$PR_NUMBER at $SHA complete"

log "[testbutton] Pruning old releases"
ls -t ~/releases/ | grep -v '^current$' | tail -n +4 | xargs -I{} rm -rf ~/releases/{}
