# PR 3 — Contact Page UI

Depends on PR 2. Adds the "Notification Settings" section to `/contact` and the toast feedback on save.

## Contact Page Layout

The `/contact` GET handler already renders name + phone in a table. Add a new section below it with a
`<form method="post" action="/contact/preferences">` containing three sub-sections. The form has a single
submit button at the bottom: "Save settings".

### 1 — Opt-out toggle

A Bootstrap form-check switch:

```
[toggle] Receive text messages from The Button
```

Renders checked when `notificationPreferences.notificationsEnabled == true`. The checkbox `name="notificationsEnabled"`.
Because HTML checkboxes only send a value when checked (absent = false), this is correct without a hidden field.

When unchecked, show a muted note: "You won't receive any texts until you turn this back on."

### 2 — Snooze

Show only when `notificationsEnabled`. A button group (Bootstrap `btn-group`) with five options:

```
Snooze for:  [None]  [1 day]  [7 days]  [30 days]  [90 days]
```

Rendered as radio inputs styled as buttons (`btn-check` + `btn-outline-secondary`). `name="snoozePreset"`,
values `"none"`, `"1"`, `"7"`, `"30"`, `"90"`.

If `snoozedUntil` is non-null and in the future, pre-select the nearest matching preset (or just show
"None" pre-selected and display a note: "Snoozed until {date}" below the group). Include a server-side
formatted date for the expiry (format as e.g. `"Jun 7"` or `"Jun 7, 2026"` if different year).

### 3 — Quiet hours

Show only when `notificationsEnabled`. A row with two `<input type="time">` fields and a timezone `<select>`:

```
Don't text me between [23:00] and [07:00]   [Eastern Time ▼]
```

Field names: `quietHoursStart`, `quietHoursEnd`, `timezone`. If quiet hours are not set, fields are empty.
Clearing both time fields removes quiet hours on save.

**Timezone selector**: A `<select name="timezone">` with a curated ~35 option list. Show four US zones first,
then a separator, then the rest alphabetically by region. Use friendly display names:

```
Eastern Time (US)         America/New_York
Central Time (US)         America/Chicago
Mountain Time (US)        America/Denver
Pacific Time (US)         America/Los_Angeles
--- (optgroup separator)
Alaska                    America/Anchorage
Hawaii                    Pacific/Honolulu
Atlantic Time (Canada)    America/Halifax
London / UTC              Europe/London
Central Europe            Europe/Paris
Eastern Europe            Europe/Helsinki
Moscow                    Europe/Moscow
Dubai                     Asia/Dubai
India                     Asia/Kolkata
Bangkok                   Asia/Bangkok
Singapore / HK            Asia/Singapore
Tokyo                     Asia/Tokyo
Sydney                    Australia/Sydney
Auckland                  Pacific/Auckland
...
```

Pre-select the saved `timezone` value when set. When not yet set, pre-select using the browser's timezone
via a small inline script (run after DOM load):

```javascript
const sel = document.getElementById('timezone-select');
if (!sel.value) {
    const tz = Intl.DateTimeFormat().resolvedOptions().timeZone;
    const opt = sel.querySelector(`option[value="${CSS.escape(tz)}"]`);
    if (opt) sel.value = tz;
}
```

## Toast Feedback

The GET `/contact` handler checks for a `saved` query parameter. When present, include a Bootstrap Toast
in the rendered HTML with a success message ("Settings saved."). Initialize it with an inline script:

```html
<div class="toast-container position-fixed bottom-0 end-0 p-3">
  <div id="savedToast" class="toast align-items-center text-bg-success border-0" role="alert">
    <div class="d-flex">
      <div class="toast-body">Settings saved.</div>
      <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
    </div>
  </div>
</div>
<script>
  new bootstrap.Toast(document.getElementById('savedToast')).show();
</script>
```

## Tests

- Vitest: test the inline timezone-detection script in jsdom (mock `Intl.DateTimeFormat`)
- Manual smoke test on testbutton: set each preference, submit, verify toast, reload and verify values persist
- Verify opt-out toggle hides snooze + quiet hours sections (can use CSS `display:none` controlled by
  a small `change` listener, or just let the next page load reflect the saved state — simpler)
