# PR 4: Frontend Display

## Goal
Display daily stats below the "BUTTON PRESSERS: N" heading.

## Files to modify

### `src/main/kotlin/sh/zachwal/button/home/HomeController.kt`
Add `id="dailyStats"` and `id="dailyStatsWhite"` elements under `#buttonPressCount`,
matching the existing two-element pattern for the fireworks background. Start empty —
stats arrive on the first WebSocket message.

### `frontend/src/main/js/net/socket.js`
```js
case 'DailyStats':
  this._handlers.onDailyStats?.(msg)
  break
```

### `frontend/src/main/js/bootstrap/main.js`
```js
function setDailyStats({ uniquePressers, peakConcurrent, totalPresses }) {
  const text = `${uniquePressers} pressers today · peak ${peakConcurrent} · ${totalPresses} presses`
  document.getElementById('dailyStats').innerText = text
  document.getElementById('dailyStatsWhite').innerText = text
}

// In socket handlers:
onDailyStats: (msg) => setDailyStats(msg),
// In onSnapshot handler, add:
setDailyStats(msg.dailyStats)
```

### CSS
Style `#dailyStats` and `#dailyStatsWhite` with smaller text under the main counter.
Starting point: `font-size: 0.6em; opacity: 0.8; margin-top: 0.25em;`

## Tests
Add frontend tests for `setDailyStats` rendering and `DailyStats` socket message dispatch.

## Verification
```bash
npm --prefix frontend test
./gradlew build
```
