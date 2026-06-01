# Contact Notification Preferences

Design document for giving contacts self-service control over SMS notifications.

## Goals

Let a contact:
1. **Opt out permanently** — turn off button texts entirely
2. **Snooze** — suppress texts for a fixed duration (1d, 7d, 30d, 90d)
3. **Quiet hours** — suppress texts during a nightly window in their local timezone (e.g. 11 pm – 7 am)

All controls live on the `/contact` page, which is already auth-gated behind `ContactSessionPrincipal`.

---

## Current State

| Area | What exists |
|---|---|
| `contact` table | `id`, `created_date`, `name`, `phone_number`, `active` |
| Opt-out mechanism | Admin can flip `active = false` via `/admin/contacts` — but this conflates admin deactivation with user opt-out |
| Filtering point | `ContactNotifier.contactsToNotify()` calls `contactDAO.selectActiveContacts()`, then sorts by 90-day press activity |
| Rate limiting | Global: at most one notification batch per 24 h (`notificationDAO.getLatestNotification()`); also per-contact 30-day rolling limit in `ControlledContactMessagingService` |
| Contact page | Shows name + phone only; no preference controls |

There is no per-contact preference state of any kind today.

---

## Data Model

### Migration `15_add_contact_notification_prefs.json`

Add four nullable columns to the `contact` table:

| Column | Type | Default | Purpose |
|---|---|---|---|
| `notifications_enabled` | `BOOLEAN` | `true` | Permanent opt-out flag (user-controlled; distinct from admin `active` flag) |
| `snoozed_until` | `TIMESTAMPTZ` | `NULL` | When non-null and in the future, contact is snoozed |
| `quiet_hours_start` | `TIME` | `NULL` | Local time of day when quiet period begins (e.g. `23:00`) |
| `quiet_hours_end` | `TIME` | `NULL` | Local time of day when quiet period ends (e.g. `07:00`) |
| `timezone` | `VARCHAR(64)` | `NULL` | IANA tz string for quiet-hours evaluation (e.g. `America/New_York`) |

`quiet_hours_start`, `quiet_hours_end`, and `timezone` should all be set or all be null — add a CHECK constraint:

```sql
CHECK (
    (quiet_hours_start IS NULL AND quiet_hours_end IS NULL AND timezone IS NULL) OR
    (quiet_hours_start IS NOT NULL AND quiet_hours_end IS NOT NULL AND timezone IS NOT NULL)
)
```

**Why separate from `active`?**  
`active` is an admin control used to permanently remove someone from the list (e.g. bad number, unsubscribed via SMS). `notifications_enabled` is a user-facing toggle. Keeping them separate means an admin deactivation is not accidentally overridden by a user re-enabling, and the admin UI stays clean.

---

## Backend Changes

### 1. `Contact` data class

Add fields mirroring the new columns:

```kotlin
data class Contact(
    val id: Int,
    val createdDate: Instant,
    val name: String,
    val phoneNumber: String,
    val active: Boolean,
    val notificationsEnabled: Boolean = true,
    val snoozedUntil: Instant? = null,
    val quietHoursStart: LocalTime? = null,
    val quietHoursEnd: LocalTime? = null,
    val timezone: String? = null,
)
```

### 2. `ContactDAO`

Add:

```kotlin
fun updateNotificationPreferences(
    contactId: Int,
    notificationsEnabled: Boolean,
    snoozedUntil: Instant?,
    quietHoursStart: LocalTime?,
    quietHoursEnd: LocalTime?,
    timezone: String?,
): Contact?
```

Use `@SqlUpdate` with named bind parameters, returning the updated row.

### 3. `ContactNotifier.contactsToNotify()` — filtering logic

After fetching active contacts, apply preference filters:

```kotlin
private fun contactsToNotify(): List<Contact> {
    val now = Instant.now()
    val contacts = contactDAO.selectActiveContacts()
        .filter { it.notificationsEnabled }
        .filter { c -> c.snoozedUntil?.isAfter(now) != true }
        .filter { c -> !isInQuietHours(c, now) }
    // ... existing sort by press counts
}

private fun isInQuietHours(contact: Contact, now: Instant): Boolean {
    val tz = contact.timezone ?: return false
    val start = contact.quietHoursStart ?: return false
    val end = contact.quietHoursEnd ?: return false
    val localTime = now.atZone(ZoneId.of(tz)).toLocalTime()
    return if (start <= end) {
        localTime >= start && localTime < end
    } else {
        // Wraps midnight: e.g. 23:00–07:00
        localTime >= start || localTime < end
    }
}
```

### 4. `ContactController` — new preferences endpoint

```
POST /contact/preferences
```

Request body (JSON):

```json
{
  "notificationsEnabled": true,
  "snoozeUntil": "2026-06-07T00:00:00Z",  // null to clear
  "quietHoursStart": "23:00",             // null to clear
  "quietHoursEnd": "07:00",               // null to clear
  "timezone": "America/New_York"          // null to clear
}
```

Returns `200 OK` with the updated contact or `400` on validation error.

**Validation:**
- If `quietHoursStart` or `quietHoursEnd` is provided, `timezone` must also be provided.
- `timezone` must be a valid IANA zone ID (catch `ZoneRulesException` from `ZoneId.of()`).
- `snoozeUntil` must be in the future if provided.

---

## Frontend Changes (Contact Page)

The `/contact` page uses Kotlinx.html DSL. Add a new "Notification Settings" section below the contact info table.

### Opt-out toggle

A simple Bootstrap switch rendered server-side with the current `notificationsEnabled` state:

```
[ ] Receive button text messages
```

Disabling this shows a confirmation message: "You won't receive any more texts from The Button."

### Snooze

Show only when notifications are enabled. A button group or select:

```
Snooze for: [1 day] [7 days] [30 days] [90 days]
```

If currently snoozed, show the expiry and a "Resume now" button.

### Quiet hours

Show only when notifications are enabled. Two `<input type="time">` fields + a timezone selector:

```
Don't text me between [23:00] and [07:00]  Timezone: [America/New_York ▼]
```

Timezone selector: a `<select>` with a curated list of IANA zones (cover US, EU, AU timezones — maybe 30–40 options). Can populate from a static list in Kotlin HTML generation.

### Interaction model

All three sections submit via a single `POST /contact/preferences` JSON call with JavaScript `fetch()`. On success, re-render or show an inline "Saved" confirmation without full page reload.

A small new JS file (`frontend/src/main/js/contact/preferences.js`) handles form serialization and the fetch call.

---

## Rollout Sequence

### Phase 1 — DB + data model (no behavior change)
1. Write `15_add_contact_notification_prefs.json` Liquibase migration
2. Update `Contact` data class + JDBI column mappings
3. Add `updateNotificationPreferences` to `ContactDAO`
4. Run migration on testbutton; verify existing contacts unaffected

### Phase 2 — Backend filtering (behavior change, contacts won't notice)
5. Add `isInQuietHours()` helper and filtering in `ContactNotifier.contactsToNotify()`
6. Add `POST /contact/preferences` route with validation
7. Write backend unit tests for `isInQuietHours()` (midnight-wrap edge case)
8. Write integration test for `POST /contact/preferences`

### Phase 3 — Contact page UI
9. Add "Notification Settings" section to `ContactController.contactInfo()`
10. Write `preferences.js` frontend module
11. Add Vitest tests for form serialization and snooze button behavior
12. Smoke-test on testbutton with a real contact session

### Phase 4 — Polish
13. Handle the case where a snoozed contact's `snoozedUntil` has passed but is still in the DB — the filter already handles this, but consider a background cleanup job or lazy-clear on read
14. Consider surfacing snooze/quiet-hours state in `/admin/contacts` cards (read-only, for admin visibility)
15. Consider emitting a Sentry breadcrumb or log entry when a contact is skipped due to prefs, for observability

---

## Open Questions

1. **Snooze implementation**: Store as `snoozed_until TIMESTAMPTZ` (absolute) vs. `(snoozed_at, snooze_duration_days)`. Absolute is simpler to filter and display.
2. **Timezone selection UX**: Full IANA picker (400+ zones) vs. curated short list vs. browser-detected default. Browser `Intl.DateTimeFormat().resolvedOptions().timeZone` can pre-select a sensible default, but we still need to persist the value.
3. **Quiet hours and the global 24h cooldown**: The notification batch fires at most once per 24 h globally. If a contact is in quiet hours when that batch fires, they miss the notification entirely (no retry). Is that acceptable, or should there be a "retry after quiet hours" mechanism? That would be significantly more complex (scheduled retry, per-contact notification tracking). Recommend: accept the miss for now — quiet hours is a "best effort" suppression, not a guaranteed delivery window.
4. **Opt-out vs. admin deactivation in the UI**: The admin `/admin/contacts` page currently shows `active` state. Should `notifications_enabled = false` also be surfaced there, or kept private to the contact? Recommend: show it read-only in the admin card for debugging.
