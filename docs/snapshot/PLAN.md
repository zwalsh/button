# Snapshot Feature Plan

## Problem

When a user connects mid-session, they never see currently pressing authenticated users in the floating presser UI. This is because `currentPressers` on the frontend is only updated by `PersonPressing` / `PersonReleased` messages, which are only sent when state changes — not on connect.

## Solution

Add a `Snapshot` server-to-client message that contains the full current state: pressing count and list of authenticated presser names. Send it:
1. **On connect** — so the new user immediately sees who is pressing
2. **Every 10 seconds** — to self-correct any drift (missed messages, reconnects, etc.)

The frontend will overwrite `currentPressers` with the snapshot list (rather than incrementally adding/removing), ensuring correct state.

## New Message: `Snapshot`

```json
{
  "type": "Snapshot",
  "count": 3,
  "names": ["Alice", "Bob"]
}
```

- `count`: total presser count (same semantics as `CurrentCount.count`)
- `names`: display names of authenticated contacts who are currently pressing (may be fewer than `count` since anonymous pressers have no name)

## Architecture

### Backend

- New `Snapshot` data class in `presser/protocol/server/`
- Registered in `ServerMessage`'s `@JsonSubTypes`
- `Presser` gets a `snapshotChannel` and `sendSnapshot(snapshot: Snapshot)` method (following the same channel-per-message-type pattern)
- `PresserManager` builds snapshots from `currentlyPressing` and sends them:
  - Immediately to the new presser in `addPresser()`
  - To all pressers every 10 seconds via a background coroutine (same pattern as `PressLogger`)

### Frontend (PR 2)

- `socket.js` routes `Snapshot` type messages to an `onSnapshot` handler
- `main.js` handles `onSnapshot` by replacing `currentPressers` with the names from the snapshot, updating the count display, and re-rendering

## Two-PR Plan

**PR 1**: Backend sends `Snapshot`. Frontend ignores unknown message types (it already does — falls to `default` case in the switch and logs an error). The `Snapshot` message will appear in browser console as "Unknown message type" until PR 2.

**PR 2**: Frontend handles `Snapshot`. After this PR, the feature is fully live.
