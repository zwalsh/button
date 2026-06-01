# Phase 4 — Admin Visibility

Depends on Phase 3. No new migration. Surfaces notification preference state on admin contact cards as
read-only information for debugging.

---

## Single PR

`AdminContactController` renders Bootstrap cards for each contact. Add a "Notifications" row to each card
showing all applicable states:

| State | Display |
|---|---|
| `notificationsEnabled = false` | Notifications off |
| `snoozedUntil` non-null and in the future | Snoozed until {date} |
| Quiet hours set | Quiet {start}–{end} {timezone display name} |
| All defaults | Notifications on |

A contact could be snoozed AND have quiet hours — show both lines.

Format `snoozedUntil` as a human-readable date (e.g. `"Jun 7"` / `"Jun 7, 2026"` for different year).

Format the timezone label using the same friendly display names as the contact page selector; fall back to
the raw IANA string if not found in the curated list.

No buttons or editing — read-only. The contact controls their own preferences via `/contact`.

### Tests

- Manual smoke test: set a snooze and quiet hours via the contact page, verify they appear on the admin card.
