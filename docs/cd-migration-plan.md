# CD Migration Plan: Decouple Deployment from Jenkins

## Goal

Allow Jenkins to run CI (build, lint, test) on ephemeral Swarm agents that are not colocated
with the production server. Deployment is handled by scripts running on the server itself,
triggered by systemd timers, using a GitHub Deploy Key for `git` access and a fine-grained
GitHub PAT for reading CI status.

## Architecture

**Before**: Jenkins agent (must be colocated) → builds tarball → copies files into `~button/` →
runs migrations → restarts systemd unit.

**After**: Jenkins agent (ephemeral, anywhere) → builds, lints, tests → sets GitHub commit
status. Meanwhile, a systemd timer on the server (running as the service user) polls for a new
green commit on the target branch, pulls source, builds locally, migrates, and restarts.

```
Jenkins (any agent)          Production server
──────────────────           ──────────────────────────────────────────────
assemble                     [timer fires every 5 min]
ktlintCheck                  deploy.sh
gradle check         ──────▶   git fetch (via deploy key)
frontend test        status    check GitHub status API (via fine-grained PAT)
setBuildStatus ◀────────────   git checkout $SHA
                               ./gradlew assemble
                               unpack into ~/releases/$SHA/
                               db/migrate.sh
                               ln -sfn ~/releases/$SHA ~/releases/current
                               sudo systemctl restart button
                               echo $SHA > ~/deployed_commit
```

## Credentials

Two separate credentials are needed on the server, owned by the service user:

| Credential       | Type            | Purpose                            | Location on server            |
|------------------|-----------------|------------------------------------|-------------------------------|
| Deploy Key       | SSH private key | `git fetch` from GitHub            | `~/.ssh/button_deploy_key`    |
| Fine-grained PAT | HTTP token      | Read commit status from GitHub API | `~/.github_token` (chmod 600) |

The fine-grained PAT should be scoped to the `zwalsh/button` repository, with only
**"Commit statuses: Read-only"** permission, and no expiration.

The deploy key is read-only (no push access needed).

Both credentials are needed on **both** `button` and `testbutton` users (they can share the same
keypair and token or use separate ones).

## Branch Targeting

- **`button`** (production): deploys the latest commit on `origin/main` only.
- **`testbutton`** (test): deploys the tip of whichever remote branch was most recently updated.

Both wait for the target commit's CI status to be `success` before deploying.

## Files Changed

### `Jenkinsfile`

Remove the `test-release`, `migrate database - testbutton`, `migrate database - button`, and
`release` stages entirely. Jenkins becomes pure CI: `start`, `assemble`, `lint`, `test`, and
`post` (test results + commit status). No deploy logic remains.

### `docs/deploy.md`

Update to describe the new CD mechanism: deploy key setup, systemd timer, deploy script
behavior, and the `~/deployed_commit` sentinel file.

### `CLAUDE.md`

Update the **Deployment & CI/CD** section to reflect that Jenkins only runs CI, and that
deployment is driven by the server-side deploy scripts and systemd timers.

## Files Added

### `scripts/deploy.sh`

Parameterized deploy script. Called by both systemd services. Accepts `--env button` or
`--env testbutton` to control which branch to target and which service to restart. Logic:

1. `git -C ~/src fetch origin`
2. Resolve target SHA:
    - `button`: `git rev-parse origin/main`
    - `testbutton`: tip of most-recently-updated remote branch (`git for-each-ref
     --sort=-committerdate`)
3. Read `~/deployed_commit`; exit 0 if already deployed
4. Query `https://api.github.com/repos/zwalsh/button/commits/$SHA/statuses`; exit 0 if not
   `success`
5. `git -C ~/src checkout $SHA`
6. `~/src/gradlew assemble` → produces `~/src/build/distributions/button.tar`
7. `mkdir -p ~/releases/$SHA && tar -xf ... -C ~/releases/$SHA`
8. Run `~/src/db/migrate.sh` (unchanged; reads credentials from `~/button.env` via `find ~`)
9. `ln -sfn ~/releases/$SHA ~/releases/current`
10. `sudo systemctl restart button` (or `testbutton`)
11. `echo $SHA > ~/deployed_commit`
12. Prune `~/releases/`: keep the 3 most recent, delete the rest

### `scripts/button-deploy.service`

Systemd service unit that runs `deploy.sh --env button` as the `button` user.

### `scripts/button-deploy.timer`

Systemd timer unit that triggers `button-deploy.service` every 5 minutes.

### `scripts/testbutton-deploy.service`

Systemd service unit that runs `deploy.sh --env testbutton` as the `testbutton` user.

### `scripts/testbutton-deploy.timer`

Systemd timer unit that triggers `testbutton-deploy.service` every 5 minutes.

## Dependencies on the Server

The following must be present for the service users on the production host:

- **JDK 17** (confirmed present)
- **Gradle wrapper** (`./gradlew` in the repo checkout — downloads Gradle automatically)
- **Liquibase** on `$PATH` (currently used by `migrate.sh`; confirm it is available to
  `button`/`testbutton` users, not just the Jenkins user)
- **`jq`** (for parsing the GitHub API JSON response)
- **`git`** on `$PATH`

## Manual Steps (One-Time Server Setup)

These cannot be scripted from this repo and must be done by hand:

### 1. Generate the deploy key

```bash
ssh-keygen -t ed25519 -f ~/.ssh/button_deploy_key -C "button-deploy" -N ""
```

Run this once; the same keypair can be placed under both `button` and `testbutton` users.

### 2. Register the public key on GitHub

In `https://github.com/zwalsh/button` → Settings → Deploy keys → Add deploy key.
Paste the contents of `~/.ssh/button_deploy_key.pub`. Select **read-only** (no write access).

### 3. Create the fine-grained PAT

In GitHub → Settings → Developer Settings → Fine-grained tokens → Generate new token:

- Resource owner: `zwalsh`
- Repository access: only `zwalsh/button`
- Permissions → Repository permissions → Commit statuses: **Read-only**
- Expiration: **No expiration**

### 4. Set up credentials on the server (repeat for both users)

As `button` (and separately as `testbutton`):

```bash
# Deploy key
mkdir -p ~/.ssh
# Copy private key content to ~/.ssh/button_deploy_key
chmod 600 ~/.ssh/button_deploy_key

# SSH config so git uses the deploy key for github.com
cat >> ~/.ssh/config <<'EOF'
Host github.com
    IdentityFile ~/.ssh/button_deploy_key
    IdentitiesOnly yes
EOF
chmod 600 ~/.ssh/config

# GitHub token
echo "ghp_xxxxxxxxxxxxxxxxxxxx" > ~/.github_token
chmod 600 ~/.github_token
```

### 5. Clone the repo on the server (repeat for both users)

```bash
git clone git@github.com:zwalsh/button.git ~/src
```

### 6. Update sudoers

The service users need permission to restart their own systemd unit. Add (via `visudo` or a
file in `/etc/sudoers.d/`):

```
button     ALL=(ALL) NOPASSWD: /bin/systemctl restart button
testbutton ALL=(ALL) NOPASSWD: /bin/systemctl restart testbutton
```

The existing Jenkins sudo rules for these restarts (and for `sudo -u button/testbutton`) can
be removed once the new scripts are confirmed working.

### 7. Install and enable the systemd timers

```bash
sudo cp scripts/button-deploy.service    /etc/systemd/system/
sudo cp scripts/button-deploy.timer      /etc/systemd/system/
sudo cp scripts/testbutton-deploy.service /etc/systemd/system/
sudo cp scripts/testbutton-deploy.timer   /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now button-deploy.timer
sudo systemctl enable --now testbutton-deploy.timer
```

### 8. Seed the deployed_commit file

To prevent the first timer firing from re-deploying the currently running version
unnecessarily, seed the sentinel with the SHA that is currently live:

```bash
# as button:
readlink ~/releases/current | xargs basename > ~/deployed_commit
# as testbutton:
readlink ~/releases/current | xargs basename > ~/deployed_commit
```

## Rollout Order

1. Implement and merge Jenkinsfile changes (CI-only) — Jenkins stops deploying.
2. Implement `scripts/deploy.sh` and the four systemd unit files.
3. Update `docs/deploy.md` and `CLAUDE.md`.
4. Perform the manual server setup steps (4–8 above).
5. Enable the timers and confirm a deploy fires successfully for each environment.
6. Remove legacy Jenkins sudo rules.
