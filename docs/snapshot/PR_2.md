# PR 2: Frontend — Handle Snapshot Message

## Goal

Connect the frontend to the `Snapshot` message sent by the backend. On receiving a snapshot, overwrite `currentPressers` with the snapshot's names and reset the count display.

## Prerequisite

PR 1 must be merged and deployed (or at least the backend branch must be available) so the frontend has real `Snapshot` messages to work with.

## Files to Modify

### `frontend/src/main/js/net/socket.js`

1. Add `onSnapshot` handler in the constructor:
   ```js
   this.onSnapshot = handlers.onSnapshot || function() {};
   ```
2. Add a `case` in `_handleMessage`:
   ```js
   case 'Snapshot':
     this.onSnapshot(msg);
     break;
   ```
3. Update the class comment at the top to include `Snapshot`.

### `frontend/src/main/js/bootstrap/main.js`

Add an `onSnapshot` handler to the `Socket` constructor:

```js
onSnapshot: (msg) => {
    currentPressers.clear();
    msg.names.forEach(name => currentPressers.add(name));
    renderFloatingPressers(Array.from(currentPressers));

    let buttonPressDiv = document.getElementById("buttonPressCount");
    let buttonPressDivWhite = document.getElementById("buttonPressCountWhite");
    if (buttonPressDiv) buttonPressDiv.innerText = "BUTTON PRESSERS: " + msg.count;
    if (buttonPressDivWhite) buttonPressDivWhite.innerText = "BUTTON PRESSERS: " + msg.count;
},
```

The count-update logic is the same as `onCurrentCount` — you may want to extract a small helper to avoid duplication, but that's optional.

## Tests

### `frontend/src/__tests__/socket.test.js` (or wherever socket tests live)

Add a test that when a `Snapshot` message arrives, the `onSnapshot` handler is called with the parsed message.

### `frontend/src/__tests__/main.test.js` (or equivalent integration test)

Add tests:
1. **Snapshot overwrites pressers** — given `currentPressers = { 'Alice' }`, receiving a snapshot with `names: ['Bob', 'Carol']` results in `currentPressers = { 'Bob', 'Carol' }` and re-renders.
2. **Snapshot updates count** — receiving a snapshot with `count: 5` updates both count DOM elements.
3. **Snapshot with empty names** — receiving a snapshot with `names: []` clears all pressers.

## Notes

- The `onSnapshot` handler in `socket.js` should default to a no-op (same as the other handlers) so the class is backwards-compatible.
- The snapshot replaces `currentPressers` entirely — do not add to the set, replace it. This ensures stale state (e.g., after a reconnect) is corrected.
- The 10-second periodic snapshot from the backend means the frontend will self-correct within 10 seconds even if individual `PersonPressing`/`PersonReleased` events were missed.
