# Phase 1 — Opt-Out Toggle

**Status: Shipped.** PR 1a merged as "Phase 1a: notifications_enabled column, NotificationPreferences model,
ContactDAO" (0614024), PR 1b merged as "Phase 1b: notification filtering, preferences endpoint, settings UI"
(c83249f).

Ships the `notifications_enabled` flag end-to-end: DB → filtering → contact page toggle.

A few implementation details differ from the plan below; see the note at the end of each PR section.

---

## PR 1a — Migration, Model, DAO

### Migration: `15_add_notifications_enabled.json`

Add one column to `contact`:

```sql
ALTER TABLE contact
    ADD COLUMN notifications_enabled BOOLEAN NOT NULL DEFAULT true;
```

Follow the addColumn changeset format from existing migrations (see `db/11_create_contact_press_counts.json`).

### New data class: `NotificationPreferences` (`db/jdbi/NotificationPreferences.kt`)

Start with just this one field; later phases add to it.

```kotlin
data class NotificationPreferences(
    // TODO: Default to false once the opt-in onboarding flow is built — see PhoneBookService.register()
    val notificationsEnabled: Boolean,
)
```

### Update `Contact` data class

Add the nested preferences. Do NOT add Kotlin defaults to fields; this does not integrate well with JDBI.

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

`@Nested` is `org.jdbi.v3.core.mapper.Nested`. With `KotlinPlugin` installed, `KotlinMapper` should map
the flat column `notifications_enabled` to `NotificationPreferences.notificationsEnabled` automatically.

**Fallback if `@Nested` doesn't work**: add `notificationsEnabled: Boolean` as a flat field on `Contact`
and skip the `NotificationPreferences` wrapper for now. Verify with a simple DAO integration test first.

### Update `ContactDAO`

Add an update method. Since the form (PR 1b) will submit all preference fields together, keep a single
update method that grows with each phase:

```kotlin
@SqlUpdate(
    """
    UPDATE contact SET notifications_enabled = :notificationsEnabled
    WHERE id = :contactId
    RETURNING *
"""
)
fun updateNotificationPreferences(
    @Bind("contactId") contactId: Int,
    @Bind("notificationsEnabled") notificationsEnabled: Boolean,
): Contact?
```

All existing queries (`selectActiveContacts`, `selectContacts`, `findContact`, `createContact`) continue to
work — the new column has a server-side default and is returned via `SELECT *`.

### Tests

- Integration test: create a contact, call `updateNotificationPreferences(id, false)`, assert returned
  `Contact` has `notificationPreferences.notificationsEnabled == false`.
- Assert existing contacts loaded after migration have `notificationsEnabled = true`.

**As shipped:** matches the plan as written — `@Nested` worked with `KotlinMapper`, no fallback needed.
Tests live in `ContactDAOTest`.

---

## PR 1b — Filtering, Endpoint, UI

### `ContactNotifier.contactsToNotify()`

Add one filter after `contactDAO.selectActiveContacts()`:

```kotlin
.filter { it.notificationPreferences.notificationsEnabled }
```

Log at DEBUG when a contact is skipped: `"Skipping contact ${c.id}: notifications disabled"`.

**As shipped:** filter lives inline in `contactsToNotify()` (`ContactNotifier.kt`) rather than a bare
`.filter { ... }` chain call, and the skip log is at INFO, not DEBUG: `"Skipping contact id=${c.id}
name=${c.name}: notifications disabled"`.

### `POST /contact/preferences`

Add to `ContactController`. Receives a standard HTML form POST. For Phase 1, only one field matters:

| Form field             | Type     | Notes                          |
|------------------------|----------|--------------------------------|
| `notificationsEnabled` | checkbox | Present = true, absent = false |

Handler:

1. Parse `notificationsEnabled` (checkbox present → true, absent → false).
2. Call `contactDAO.updateNotificationPreferences(contactId, notificationsEnabled)`.
3. Redirect to `GET /contact?saved=true`.

The endpoint is designed to be extended in later phases (snooze, quiet hours fields will be added to the
same form and handler).

### Contact page UI

Add a "Notification Settings" section to the `contactInfo()` GET handler, below the contact info table.
Render a `<form method="post" action="/contact/preferences">` with:

- A Bootstrap form-check switch: `[toggle] Receive text messages from The Button`
    - `name="notificationsEnabled"`, checked when `notificationPreferences.notificationsEnabled == true`
    - When unchecked: muted note "You won't receive any texts until you turn this back on."
- A submit button: "Save settings"

Later phases will add snooze and quiet hours sections to this same form.

**As shipped:** the settings page is the existing `GET /contact` route (auth-gated by `contactRoute` /
`CONTACT_SESSION_AUTH`, so it is not publicly accessible), renamed to page title "Settings" with an
`h1` "Settings" heading. `notificationSettingsCard()` and `contactInfoCard()` in `ContactController.kt`
render the info table and the toggle card. The submit button reads "Save", not "Save settings".

### Toast on save

In `GET /contact`, check for `?saved=true` query param. When present, render a Bootstrap toast:

```html
<div class="toast-container position-fixed bottom-0 end-0 p-3">
    <div id="savedToast" class="toast align-items-center text-bg-success border-0" role="alert">
        <div class="d-flex">
            <div class="toast-body">Settings saved.</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    </div>
</div>
```

Add a new js file to trigger the toast following `frontend/README.md` patterns.

**As shipped:** implemented as a dismissible Bootstrap alert (`savedAlert()`, `#savedAlert`,
`alert alert-success ... fixed-bottom`) rather than a toast component, still triggered by `?saved=true`.
JS lives at `frontend/src/main/contact/toast.js`, served as `/static/src/contact/toast.js`.

### Tests

- Integration test for `POST /contact/preferences`: checkbox present → `notificationsEnabled = true`;
  absent → false; successful post redirects to `/contact?saved=true`.
- Manual smoke test on testbutton: toggle off, submit, reload, verify state persists and toast appears.

**As shipped:** `ContactControllerTest` covers the endpoint; `ContactNotifierTest` covers the filtering.
