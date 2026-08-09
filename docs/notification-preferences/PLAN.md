# Contact Notification Preferences — Plan

## Goals

Give contacts self-service control over SMS notifications from the `/contact` page:

1. **Opt out permanently** — turn off button texts entirely
2. **Snooze** — suppress texts for a fixed duration (1d, 7d, 30d, 90d)
3. **Quiet hours** — suppress texts during a nightly window in their local timezone (e.g. 11 pm – 7 am)

## Current State

_Last updated 2026-08-09, after Phase 2a shipped, PR 2b in progress, PR 3a in progress._

**Phase 1 (opt-out toggle) is shipped** — see `PHASE_1.md` for as-built details. The `contact` table now has
a `notifications_enabled` column (migration `15_add_notifications_enabled.json`), exposed on `Contact` via a
nested `NotificationPreferences` model. `ContactNotifier.contactsToNotify()` filters out contacts with
notifications disabled before sending. The `/contact` page (auth-gated behind `ContactSessionPrincipal`,
token-based from SMS link — not publicly accessible) now has a "Settings" heading with a Notification
Settings card containing the opt-out toggle, which posts to `/contact/preferences` and redirects back with a
dismissible success alert.

**Phase 2a (snooze migration, model, DAO) is shipped** — see `PHASE_2.md` for as-built details. The `contact`
table now has a nullable `snoozed_until` column (migration `16_add_snoozed_until.json`), added to
`NotificationPreferences` as `snoozedUntil: Instant?` and to `ContactDAO.updateNotificationPreferences`.
PR 2b (filtering, endpoint, UI) is in progress.

**Phase 3a (quiet hours migration, model, DAO) is in progress** — see `PHASE_3.md`. The `contact` table now
has `quiet_hours_start`/`quiet_hours_end` (nullable `time`) and `timezone` (nullable `VARCHAR(64)`, defaults
to `America/New_York`) columns (migration `17_add_quiet_hours.json`), with a CHECK constraint requiring
`timezone` whenever either quiet-hours column is set. `NotificationPreferences` gained
`quietHoursStart`/`quietHoursEnd`/`timezone`, and `ContactDAO.updateQuietHours` updates all three together.
PR 3b (filtering only — split from the originally-planned filtering+endpoint+UI PR, see `PHASE_3.md`) is
in progress. PR 3c (endpoint, UI) has not started.

Phase 4 (admin visibility) is not started.

The `active` flag remains the separate admin-controlled suppression mechanism described below.

## Architectural Decisions

### `notifications_enabled` is separate from `active`

`active` is an admin control (bad number, hard unsubscribe). `notifications_enabled` is a user-facing toggle.
They must stay separate so an admin deactivation cannot be overridden by the user, and vice versa. Contacts
can see their `active` status but cannot change it.

### Preference columns live on the `contact` table

The relationship is 1:1 and always-present (every contact has preferences). Adding columns to `contact` avoids
a join and a missing-row edge case. A separate table would be appropriate if preferences were optional or needed
independent audit history; neither applies here.

The Kotlin model uses an embedded `NotificationPreferences` data class on `Contact` with JDBI's `@Nested`
annotation. If `@Nested` proves incompatible with `KotlinMapper` at the current JDBI version, fall back to
flat fields on `Contact`.

### Filtering point

All preference checks are applied in `ContactNotifier.contactsToNotify()`, after fetching active contacts
and before sorting by press counts.

### Quiet hours are best-effort

Notifications fire at most once per 24 h globally. If a contact is in quiet hours when the batch fires, they
miss it — no retry. This is acceptable.

### Contact page uses a standard HTML form

`POST /contact/preferences` with a redirect back to `GET /contact?saved=true`. The GET handler renders a
Bootstrap success toast when `saved=true` is present. A small inline `<script>` calls `bootstrap.Toast.show()`.

### Timezone detection

For the quiet hours timezone selector, the browser API `Intl.DateTimeFormat().resolvedOptions().timeZone`
returns the user's local IANA zone string (e.g. `"America/New_York"`). Use it to pre-select the right
`<option>` in the timezone `<select>` before the user has saved a preference. After saving, the server
renders the stored value as selected.

## Phases

Each phase ships one end-to-end working feature. Within each phase, the migration PR deploys first.

| Phase | What ships | PRs | Status |
|-------|-----------|-----|--------|
| 1 | Opt-out toggle | 1a: migration + model + DAO · 1b: filtering + endpoint + UI | Shipped |
| 2 | Snooze | 2a: migration + model + DAO · 2b: filtering + endpoint + UI | 2a shipped, 2b in progress |
| 3 | Quiet hours | 3a: migration + model + DAO · 3b: filtering · 3c: endpoint + UI | 3a in progress, 3b in progress |
| 4 | Admin visibility | Single PR: read-only prefs on admin contact cards | Not started |

Phases deploy in order. Each PR is independently mergeable and safe to run on testbutton before production.
