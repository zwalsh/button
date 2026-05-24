#!/bin/bash
set -euo pipefail

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

# 3. Exit if already deployed
DEPLOYED_FILE=~/deployed_commit
if [[ -f "$DEPLOYED_FILE" ]] && [[ "$(cat "$DEPLOYED_FILE")" == "$SHA" ]]; then
    exit 0
fi

# 4. Exit if CI status is not success
STATUS=$(curl -s "https://api.github.com/repos/zwalsh/button/commits/$SHA/status" \
    -H "Authorization: token $TOKEN" | jq -r '.state // empty') || true
if [[ "$STATUS" != "success" ]]; then
    exit 0
fi

# 5. Check out the target commit
git -C "$REPO" checkout -f "$SHA"

# 6. Build
"$REPO/gradlew" -p "$REPO" assemble

# 7. Unpack into release directory
RELEASE_DIR=~/releases/$SHA
mkdir -p "$RELEASE_DIR"
tar -xf "$REPO/build/distributions/button.tar" -C "$RELEASE_DIR"

# 8. Run database migrations
"$REPO/db/migrate.sh"

# 9. Atomically update the current symlink
ln -sfn "$RELEASE_DIR" ~/releases/current

# 10. Restart the service
sudo systemctl restart "$ENV"

# 11. Record the deployed commit
echo "$SHA" > "$DEPLOYED_FILE"

# 12. Prune old releases, keeping the 3 most recent
ls -t ~/releases/ | grep -v '^current$' | tail -n +4 | xargs -I{} rm -rf ~/releases/{}
