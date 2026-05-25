# Admin Contacts Page Improvements

## Goal

Make the `/admin/contacts` page more useful for managing who receives SMS notifications:

- Search bar + active/inactive filter (SSR, query params, View Transitions API)
- Last-90-days press count column (sorted descending)
- Card-based mobile layout

---

## Current State

- `AdminContactController.contactPage()` calls `phoneBookService.contacts()` and renders a table
- Table columns: Name, Number, Active (emoji), Update (Activate/Deactivate button)
- `update.js` POSTs to `/admin/contacts/update` and reloads on success
- Bootstrap 4.4.1 is available via `headSetup()`; jQuery 3.5.1 is already included on this page
- `ContactPressStatsService.pressStats(TimeRange)` returns `List<ContactPressStat>` sorted by count desc, **only for
  contacts with ≥ 1 press**
- `TimeRange` options: TODAY, LAST_7_DAYS, LAST_30_DAYS, YEAR_TO_DATE, ALL_TIME — no 90-day option yet

---

## Plan

### Session 1 — Backend: 90-day press stats + SSR search/filter

**1a. Add `LAST_90_DAYS` to `TimeRange`**

File: `src/main/kotlin/sh/zachwal/button/admin/ContactPressStatsService.kt`

Add `LAST_90_DAYS("Last 90 Days", "90d")` to the enum. Confirm the materialized DAO query and today's real-time query
both accept a flexible lookback `Instant` so this just works.

Check: `src/main/kotlin/sh/zachwal/button/db/dao/` for the DAO used by `ContactPressStatsService`.

**1b. Merge press stats with full contact list**

`pressStats()` omits zero-press contacts. The page needs all contacts. Change `AdminContactController.contactPage()` to:

```kotlin
val allContacts = phoneBookService.contacts()
val stats = contactPressStatsService.pressStats(TimeRange.LAST_90_DAYS)
    .associateBy { it.contact.id }
val contactRows = allContacts
    .map { c -> ContactRow(c, stats[c.id]?.count ?: 0L) }
    .sortedByDescending { it.pressCount }
```

Add a local `data class ContactRow(val contact: Contact, val pressCount: Long)` inside the controller (private, no need
for a shared file).

**1c. Read query params and filter server-side**

The GET handler reads two optional query params:

- `query`: plain text, OR-matched against `contact.name` and `contact.phoneNumber` (case-insensitive)
- `active`: `true` or `false`; absent means show all

```kotlin
val query = call.request.queryParameters["query"]?.lowercase()
val activeFilter = call.request.queryParameters["active"]?.toBooleanStrictOrNull()

val filtered = contactRows
    .filter { row ->
        query == null ||
                row.contact.name.lowercase().contains(query) ||
                row.contact.phoneNumber.contains(query)
    }
    .filter { row ->
        activeFilter == null || row.contact.active == activeFilter
    }
```

Pass `query` and `activeFilter` back to the template so the search box and filter buttons render with their current
state.

**1d. Search bar + filter UI**

Above the table, render a `<form method="get" action="/admin/contacts">` with:

- A text `<input>` pre-filled with the current `query` value
- Three filter buttons (All / Active / Inactive) rendered as submit buttons or links with the appropriate `active`
  param — the selected one gets `btn-primary`, others get `btn-outline-secondary`
- Auto-submit via a small JS snippet on keyup with a short debounce (250ms)

Using a plain `<form>` GET keeps the URL bookmarkable and the back button sensible.

**1e. View Transitions API**

Add `<meta name="view-transition" content="same-origin">` (or the equivalent `<link>`) in the `<head>` so the browser
animates between the current and filtered page state. No JS routing needed — the browser handles the cross-document
transition automatically when both pages opt in.

This gives a smooth fade/slide when submitting the search form without any client-side complexity.

**1f. Add press count to the table**

Add a new "Presses (90d)" column header and render `contactRow.pressCount` as a `td`. The list comes pre-sorted from the
server so no client-side sort needed.

**1g. Inject `ContactPressStatsService`**

Add it to the `AdminContactController` constructor — it's already a Guice-managed singleton.

---

### Session 2 — Mobile UX: card layout

The table overflows on mobile. Replace it with a responsive card list that's shown on small screens while the table
remains on larger ones (or replace the table entirely if the card layout reads well on desktop too — decide when
implementing).

**Card structure per contact:**

```
┌─────────────────────────────────────────┐
│ Name Surname          ✅  47 presses    │
│ +1 (555) 123-4567        [Deactivate]   │
└─────────────────────────────────────────┘
```

- **Left side**: name in bold on the first line, phone number in muted text below (`text-muted`)
- **Right side**: active emoji + press count on the first line, action button below — right-aligned

Bootstrap implementation: each card uses `d-flex justify-content-between align-items-center` inside a `card-body`. No JS
changes needed — the existing `data-contact-id` / `data-contact-active` attributes on the button work unchanged with
`update.js`.

**Other mobile details:**

- Search form and filter buttons are already full-width `form-control` / block-level — no extra work needed
- The card list sits inside the existing `.container` div

---

## Deferred / Out of Scope

- Real-time row updates after Activate/Deactivate (currently reloads — keep that behavior; the reload also re-applies
  current query params naturally)
- Sorting by other columns
- Pagination

---

## File Checklist

| File                                                                            | Session | Change                                                                                                     |
|---------------------------------------------------------------------------------|---------|------------------------------------------------------------------------------------------------------------|
| `src/main/kotlin/sh/zachwal/button/admin/ContactPressStatsService.kt`           | 1       | Add `LAST_90_DAYS` to `TimeRange`                                                                          |
| `src/main/kotlin/sh/zachwal/button/admin/contact/AdminContactController.kt`     | 1       | Inject service, merge + filter data, read query params, render search form + table with press count column |
| `src/main/kotlin/sh/zachwal/button/sharedhtml/Head.kt` or controller head block | 1       | Add View Transitions meta tag                                                                              |
| Any DAO that `ContactPressStatsService` uses                                    | 1       | Verify/update for 90-day lookback                                                                          |
| `src/main/kotlin/sh/zachwal/button/admin/contact/AdminContactController.kt`     | 2       | Replace/augment table with card layout                                                                     |
| `frontend/src/main/admin/contacts/update.js`                                    | 2       | Auto-submit search form on input (optional, ~5 lines)                                                      |
