# Deploy Instructions

Runs as a systemd unit (see [button.service](../button.service)).

Requires its own user. The systemd service file specifies a binary within that user's home
directory, which is assembled by `./gradlew build` with the `application` Gradle plugin.

## Dependencies

Depends on a Postgres database server. Its schema is controlled by liquibase. See [db](../db) and
[liquibase.properties](../db/liquibase.properties). The database connection information (that
isn't configured via environment variables) is configured in
[hikari.properties](../src/main/resources/hikari.properties).

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
