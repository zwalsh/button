# CD Migration: GitHub Releases-Based Deployment

## Goal

Eliminate server-side compilation. Jenkins builds the tarball once, uploads it as a GitHub
release, and production pulls the pre-built artifact. Test deployments remain source-based but
become intentional (label-triggered) rather than automatic.

## Motivation

The current `deploy.sh` compiles the Kotlin codebase on the server for every deploy, which is
memory-intensive. This also means three separate compilations per main merge: Jenkins, button,
and testbutton. Moving to pre-built releases removes the JDK/Gradle runtime requirement from
the server entirely for production, and reduces test builds to on-demand only.

## Architecture

```
Jenkins (any agent)                    Production server
──────────────────                     ─────────────────────────────────────────
assemble                               [timer fires every 1 min]
ktlintCheck                            prod-deploy.sh
gradle check             ──────▶         gh release view --latest → download URL
frontend test            release         curl -L <asset-url> -o button.tar
gh release create ◀─────────────         tar -xf into ~/releases/$TAG/
setBuildStatus                           db/migrate.sh
                                         ln -sfn ~/releases/$TAG ~/releases/current
                                         sudo systemctl restart button
                                         echo $TAG > ~/deployed_commit

Developer labels PR "deploy-test"      Test server (manual/intentional)
──────────────────────────────────     ────────────────────────────────────────
                                       test-deploy.sh
                                         gh pr list --label deploy-test
                                         find most-recently-updated PR
                                         check CI status = success for HEAD SHA
                                         git checkout $SHA
                                         ./gradlew assemble
                                         unpack, migrate, restart testbutton
```

## Changes to Existing Files

### `Jenkinsfile` (DONE)

Add a `release` stage between `test` and `setBuildStatus`, running only on `main`:

```groovy
stage('release') {
    when { branch 'main' }
    steps {
        sh 'gh release create "sha-${GIT_COMMIT}" \
              build/distributions/button.tar \
              --title "sha-${GIT_COMMIT}" \
              --notes "" \
              --latest'
    }
}
```

The `gh` CLI must be available on agents (install in agent image or via `apt`). Jenkins needs a
credential with `Contents: write` stored as an environment variable (`GITHUB_TOKEN`) or
`gh auth` pre-configured on the agent.

`setBuildStatus` fires after `release`, so the asset exists before CI is marked green.

### `scripts/deploy.sh` (prod only, simplified) (DONE)

Replace the checkout + build + unpack block with a download:

1. `gh release view --latest --json tagName,assets` → get tag name and asset download URL
2. Read `~/deployed_commit`; exit 0 if tag already deployed
3. Query GitHub commit status API; exit 0 if not `success`
4. `curl -L <asset-url> -o /tmp/button.tar`
5. `mkdir -p ~/releases/$TAG && tar -xf /tmp/button.tar -C ~/releases/$TAG`
6. `~/button/db/migrate.sh`
7. `ln -sfn ~/releases/$TAG ~/releases/current`
8. `sudo systemctl restart button`
9. `echo $TAG > ~/deployed_commit`

The `GITHUB_TOKEN` on the server does not need auth for asset downloads since the repo is
public — `curl` can fetch the asset URL directly with no credentials. The existing
`~/.github_token` is still needed for the status API check (avoids rate limiting on unauthenticated
requests).

### `scripts/test-deploy.sh` (new, replaces testbutton path in deploy.sh)

Separate script for testbutton deployments. Builds from source intentionally.

1. `gh pr list --label deploy-test --state open --json number,headRefName,headSha,updatedAt`
2. Pick the most recently updated PR
3. If none found, log and exit 0
4. Check CI status for the PR's HEAD SHA; exit 0 if not `success`
5. Read `~/deployed_commit`; exit 0 if SHA already deployed
6. `git -C ~/button fetch origin && git -C ~/button checkout -f $SHA`
7. `~/button/gradlew assemble`
8. Unpack, migrate (main-only check as before), symlink, restart testbutton, record SHA

Because this is intentional, memory pressure is isolated: testbutton only builds when a
developer explicitly labels a PR.

### `scripts/testbutton-deploy.service` / `testbutton-deploy.timer`

Update `ExecStart` to call `test-deploy.sh` instead of `deploy.sh --env testbutton`.

## Files Added

- `scripts/test-deploy.sh` — source-based deploy for testbutton, label-gated
- `docs/releases-cd/plan.md` — this file

## Credential Changes

| Credential                             | Where   | Change                                                                                               |
|----------------------------------------|---------|------------------------------------------------------------------------------------------------------|
| `GITHUB_TOKEN` (Contents: write)       | Jenkins | New — needed to create releases                                                                      |
| `~/.github_token` on button server     | Server  | No change — `Commit statuses: read` is sufficient; asset downloads are unauthenticated (public repo) |
| `~/.github_token` on testbutton server | Server  | No change — same as above, plus needs `pull_requests: read` if using `gh pr list`                    |

For `gh pr list` in `test-deploy.sh`, the token needs `Pull requests: read` in addition to the
existing `Commit statuses: read`. This likely means regenerating the testbutton server token.
Alternatively, the PAT currently used only has commit status read; `gh pr list` can be
authenticated separately or the testbutton script can use `curl` against the pulls API directly
with the same token if the scope is updated.

## Release Naming and Discovery

Releases are tagged `sha-$GIT_COMMIT` (the full 40-char SHA). `prod-deploy.sh` uses
`gh release view --latest` rather than searching by SHA — it deploys whatever the latest release
is, then checks that its SHA has green CI before switching. This is safe because Jenkins only
creates releases on `main` after a successful build, and `setBuildStatus` fires after the upload.

No pruning of GitHub releases is needed. The repo is public; release assets are stored on
GitHub's CDN free of charge with no documented count or size limit.

## Rollout Order

1. Add `gh release create` to Jenkinsfile; merge to main. Jenkins creates the first release.
2. Confirm `gh release view --latest` returns the expected asset URL.
3. Rewrite `scripts/deploy.sh` to download instead of build.
4. Write `scripts/test-deploy.sh`.
5. Update `testbutton-deploy.service` to point at `test-deploy.sh`.
6. Update credentials: new Jenkins token, update testbutton server token for PR read.
7. Test prod deploy end-to-end (re-enable `button-deploy.timer`).
8. Label a test PR and confirm testbutton picks it up.
9. Re-enable `testbutton-deploy.timer`.
10. Remove JDK/Gradle from server dependency list in `docs/deploy.md`.
