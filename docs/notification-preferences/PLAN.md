# Contact Notification Preferences — Plan

## Goals

Give contacts self-service control over SMS notifications from the `/contact` page:

1. **Opt out permanently** — turn off button texts entirely
2. **Snooze** — suppress texts for a fixed duration (1d, 7d, 30d, 90d)
3. **Quiet hours** — suppress texts during a nightly window in their local timezone (e.g. 11 pm – 7 am)

## Current State

The `contact` table has `id`, `created_date`, `name`, `phone_number`, `active`. There is no per-contact
preference state of any kind. `ContactNotifier.contactsToNotify()` sends to all active contacts. The only
suppression mechanism is the admin-controlled `active` flag, which conflates admin deactivation with user opt-out.

The `/contact` page is auth-gated behind `ContactSessionPrincipal` (token-based, from SMS link). It
currently shows name and phone number only.

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

All three preference checks are applied in `ContactNotifier.contactsToNotify()`, after fetching active
contacts and before sorting by press counts.

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

| Phase | PR | What changes | Contacts notice? |
|-------|----|-------------|-----------------|
| 1 | PR_1 | DB migration + data model | No |
| 2 | PR_2 | Backend filtering + `POST /contact/preferences` | Possibly (if any contact has prefs set, but none do yet) |
| 3 | PR_3 | Contact page UI | Yes |
| 4 | PR_4 | Admin visibility of prefs + snoozed-until | No |

Phases deploy in order. Each PR is independently mergeable and safe to run on testbutton before production.
