# PR 1 — DB Migration + Data Model

No behavior change. Adds the schema and updates the Kotlin model so later PRs have a foundation.

## DB Migration: `15_add_contact_notification_prefs.json`

Add four nullable columns to the `contact` table:

| Column | Type | Default | Purpose |
|---|---|---|---|
| `notifications_enabled` | `BOOLEAN NOT NULL` | `true` | User opt-out flag |
| `snoozed_until` | `TIMESTAMPTZ` | `NULL` | Suppress until this time |
| `quiet_hours_start` | `TIME` | `NULL` | Local start of quiet window |
| `quiet_hours_end` | `TIME` | `NULL` | Local end of quiet window |
| `timezone` | `VARCHAR(64)` | `NULL` | IANA tz for quiet-hours math |

Add a CHECK constraint so quiet hours are always fully specified or fully absent (timezone alone is allowed,
so a contact can set a timezone and later add quiet hours without losing it):

```sql
CHECK (
    (quiet_hours_start IS NULL AND quiet_hours_end IS NULL) OR
    (quiet_hours_start IS NOT NULL AND quiet_hours_end IS NOT NULL AND timezone IS NOT NULL)
)
```

Follow the existing migration JSON format (see `db/11_create_contact_press_counts.json` for a recent example
of an addColumn changeset).

## Kotlin Data Model

### `NotificationPreferences` (new file: `db/jdbi/NotificationPreferences.kt`)

```kotlin
data class NotificationPreferences(
    val notificationsEnabled: Boolean,
    val snoozedUntil: Instant?,
    val quietHoursStart: LocalTime?,
    val quietHoursEnd: LocalTime?,
    val timezone: String?,
)
```

### `Contact` — add nested preferences

```kotlin
data class Contact(
    val id: Int,
    val createdDate: Instant,
    val name: String,
    val phoneNumber: String,
    val active: Boolean,
    @Nested val notificationPreferences: NotificationPreferences,
)
```

The `@Nested` annotation is `org.jdbi.v3.core.mapper.Nested`. With `KotlinPlugin` installed, JDBI's
`KotlinMapper` should resolve constructor parameters of the nested class against the flat result-set columns
(`notifications_enabled`, `snoozed_until`, etc.).

**If `@Nested` does not work with `KotlinMapper` at JDBI 3.14.4**: fall back to flat fields on `Contact`
(no embedded class). Verify by writing a simple DAO query in the test suite and checking the mapping.

### `ContactDAO` — new update method

The existing DAO uses `@SqlQuery`/`@SqlUpdate` via `jdbi3-kotlin-sqlobject`. Add:

```kotlin
@SqlUpdate("""
    UPDATE contact SET
        notifications_enabled = :notificationsEnabled,
        snoozed_until = :snoozedUntil,
        quiet_hours_start = :quietHoursStart,
        quiet_hours_end = :quietHoursEnd,
        timezone = :timezone
    WHERE id = :contactId
    RETURNING *
""")
fun updateNotificationPreferences(
    @Bind("contactId") contactId: Int,
    @Bind("notificationsEnabled") notificationsEnabled: Boolean,
    @Bind("snoozedUntil") snoozedUntil: Instant?,
    @Bind("quietHoursStart") quietHoursStart: LocalTime?,
    @Bind("quietHoursEnd") quietHoursEnd: LocalTime?,
    @Bind("timezone") timezone: String?,
): Contact?
```

Also update `createContact` and `selectActiveContacts` / `selectContacts` queries — no SQL changes needed
(the new columns have defaults), but the mapped `Contact` type gains new fields so existing queries must
select `*` or explicitly include the new columns.

## Tests

- Add an integration test (TestContainers) that creates a contact, calls `updateNotificationPreferences`,
  and asserts the returned `Contact` has the expected `notificationPreferences` values.
- Assert that existing contacts loaded after migration have `notificationsEnabled = true` and all nullable
  fields `null`.
- Verify the CHECK constraint rejects a row with `quiet_hours_start` set but `timezone` null.
