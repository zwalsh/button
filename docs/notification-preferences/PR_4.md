# PR 4 — Admin Visibility

Depends on PR 3. Surfaces notification preference state in the admin contacts cards — read-only, for
debugging and visibility.

## Admin Contact Cards

`AdminContactController` currently renders Bootstrap cards for each contact with: name, phone, active
status (✅/❌), 90-day press count, and Activate/Deactivate buttons.

Add a "Notifications" row to each card:

| State | Display |
|---|---|
| `notificationsEnabled = false` | 🔕 Notifications off |
| `snoozedUntil` in the future | 💤 Snoozed until {date} |
| Quiet hours set | 🕐 Quiet {start}–{end} {timezone display name} |
| All defaults (notifications on, no snooze, no quiet hours) | ✉️ Notifications on |

Show all applicable states — a contact could be snoozed AND have quiet hours.

Format `snoozedUntil` as a human-readable date in the server's timezone (or UTC). Format the timezone in the
quiet hours row using the same friendly display names as the contact page selector, falling back to the raw
IANA string if not found.

## No New Routes

This is purely a rendering change to the existing `GET /admin/contacts` handler. `AdminContactController`
already receives `Contact` objects which now carry `notificationPreferences` from PR 1.

## Tests

- Unit test the `snoozedUntil` date formatting helper
- Manually verify on testbutton: set a snooze via the contact page, confirm it appears in the admin card
