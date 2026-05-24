# Deploy Instructions

Runs as a systemd unit (see [button.service](../button.service)).

Requires its own user. The systemd service file specifies a binary within that user's home
directory, which is assembled by `./gradlew assemble` with the `application` Gradle plugin.

## CI/CD Architecture

Jenkins runs CI (build, lint, test) on ephemeral agents and sets a GitHub commit status.
Deployment is handled separately by `scripts/deploy.sh`, triggered by systemd timers on the
production server. This means Jenkins does not need to be colocated with the server.

```
Jenkins (any agent)          Production server
──────────────────           ──────────────────────────────────────────────
assemble                     [timer fires every 1 min]
ktlintCheck                  deploy.sh
gradle check         ──────▶   git fetch (via deploy key)
frontend test        status    check GitHub status API (via PAT)
setBuildStatus ◀────────────   git checkout $SHA
                               ./gradlew assemble
                               unpack into ~/releases/$SHA/
                               db/migrate.sh
                               ln -sfn ~/releases/$SHA ~/releases/current
                               sudo systemctl restart button
                               echo $SHA > ~/deployed_commit
```

## Environments

- **`button`** (production): deploys the latest commit on `origin/main`.
- **`testbutton`** (test): deploys the tip of whichever remote branch was most recently updated.

Both wait for the target commit's CI status to be `success` before deploying.

## Credentials

Two credentials are required on the server, owned by the service user:

| Credential       | Type            | Purpose                            | Location                      |
|------------------|-----------------|------------------------------------|-------------------------------|
| Deploy Key       | SSH private key | `git fetch` from GitHub            | `~/.ssh/deploy_key`           |
| Fine-grained PAT | HTTP token      | Read commit status from GitHub API | `~/.github_token` (chmod 600) |

The fine-grained PAT is scoped to `zwalsh/button` with **Commit statuses: Read-only** permission.
The deploy key is read-only.

## Systemd Units

The timer units in `scripts/` fire `deploy.sh` every minute:

```bash
sudo cp scripts/button-deploy.service    /etc/systemd/system/
sudo cp scripts/button-deploy.timer      /etc/systemd/system/
sudo cp scripts/testbutton-deploy.service /etc/systemd/system/
sudo cp scripts/testbutton-deploy.timer   /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now button-deploy.timer
sudo systemctl enable --now testbutton-deploy.timer
```

View recent deploy logs:

```bash
journalctl -u button-deploy.service -n 50
journalctl -u testbutton-deploy.service -n 50
```

## Dependencies

Depends on a Postgres database server. Its schema is controlled by Liquibase. See [db](../db) and
[liquibase.properties](../db/liquibase.properties). The database connection information (that
isn't configured via environment variables) is configured in
[hikari.properties](../src/main/resources/hikari.properties).

The following must be present for the service users on the production host:

- **JDK 17**
- **Gradle wrapper** (`./gradlew` in the repo checkout — downloads Gradle automatically)
- **Liquibase** on `$PATH`
- **`jq`** (for parsing the GitHub API JSON response)
- **`git`** on `$PATH`

## Environment

Requires a `button.env` environment file in the user's home directory (loaded by systemd). That
`.env` file must specify the following environment variables:

| Variable           | Value                                                                                        |
|--------------------|----------------------------------------------------------------------------------------------|
| ENV                | The name of the current environment. Controls header configuration, HTTPS usage, etc.        |
| PORT               | The port that the server should start on.                                                    |
| HOST               | The hostname at which the server is reachable.                                               |
| WS_PROTOCOL        | `ws` or `wss` -- which protocol to use to connect to this server for a WebSocket connection. |
| DB_NAME            | The name of the Postgres database to connect to.                                             |
| DB_USER            | The user with which to connect to Postgres.                                                  |
| DB_PASSWORD        | The password with which to connect to Postgres.                                              |
| TWILIO_ACCOUNT_SID | The SID of the Twilio account to use for sending SMS. Not required.                          |
| TWILIO_AUTH_TOKEN  | The auth token of the Twilio account to use for sending SMS. Not required.                   |
| ADMIN_PHONE        | The phone number to send admin texts to.                                                     |
| SENTRY_KOTLIN_DSN  | The Sentry DSN to send Kotlin exceptions to.                                                 |
| SENTRY_JS_DSN      | The Sentry DSN to send Javascript exceptions to.                                             |
| UMAMI_URL          | The URL of the Umami host to send analytics to.                                              |
| UMAMI_WEBSITE_ID   | The website id of this deploy of the Button configured in Umami.                             |
