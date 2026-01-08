# Frontend Testing Plan

## Overview
The frontend codebase is modularized with ES modules and uses Vitest + jsdom for testing. Test coverage is no longer “minimal”: there are now unit tests for `floatingPresserPhysics`, `Pill`, and `floatingPresserPositions`. The plan below is updated to reflect what’s already covered vs. what’s still missing.

## Current Coverage
- **floatingPresserPhysics.js**:
  - `frontend/src/__tests__/floatingPresserPhysics.test.js`: basic “empty input returns an array” test.
  - `frontend/src/__tests__/js/floatingPresserPhysics.test.js`: broader coverage for initial positioning (1, 2, N pills), repel behavior (overlap, edge, soft-repel), and damping behavior as `frame` increases.
- **Pill.js**: `frontend/src/__tests__/js/Pill.test.js` covers geometry helpers, `setCenter` clamping + initialization, `setVelocity`, `createPill` truncation/class defaults, and `remove()` DOM removal (fake timers).
- **floatingPresserPositions.js**: `frontend/src/__tests__/floatingPresserPositions.test.js` covers `renderFloatingPressers` pill creation/removal/no-dup behavior (with mocked `Pill` + `computeNextPillStates`) and `assignNames` slotting rules.
- **net/socket.js**: `frontend/src/__tests__/js/net/socket.test.js` covers construction/connect, handler dispatch, outbound send gating by `readyState`, and reconnect/backoff behavior.
- **bootstrap/main.js**: `frontend/src/__tests__/js/bootstrap/main.test.js` covers event wiring (pointerdown/up), `contextmenu` prevention, and handler-to-DOM/render wiring.
- **Other modules**: Still no dedicated test coverage for `wrapped.js` or `fireworks.js`.

## Coverage Plan by Module

### 1. floatingPresserPhysics.js
- **Current**:
  - Empty input returns an array.
  - Positioning tests for 1, 2, and N pills.
  - Repel tests for overlap, edge pushback, and “soft repel”.
  - Damping behavior as `frame` increases (including MAX cap).
- **Still needed**:
  - Tests that exercise `dt`-based stepping (if/where `dt` is expected to matter) and longer-step stability.
  - More configuration/edge cases (e.g., extreme container sizes, many pills, deterministic behavior across frames).
  - Any higher-level integration that verifies the physics output is correctly applied to real `Pill` DOM elements.

### 2. Pill.js
- **Current**: Unit tests exist for geometry helpers, clamping/initialization in `setCenter`, velocity setting, pill creation defaults, and timed DOM removal in `remove()`.
- **Still needed**:
  - Additional edge cases (e.g., missing/invalid style values, zero-sized pills, container smaller than pill).
  - Any integration test coverage that uses real layout/measurements (as opposed to stubbing `offsetWidth/offsetHeight`).

### 3. floatingPresserPositions.js
- **Current**:
  - `renderFloatingPressers` creates/removes pills, avoids duplicates, and safely no-ops when required DOM nodes are missing (jsdom).
  - `assignNames` slotting rules covered (first 5 bottom, next 5 top, alternation after 10, slot reuse, and “top stays top” behavior).
  - Note: tests currently mock `Pill` and `computeNextPillStates`.
- **Still needed**:
  - Assertions that `setCenter`/`setVelocity` are called with the computed physics output.
  - A higher-fidelity integration test with the real `Pill` implementation (optional, but would catch DOM/measurement regressions).

### 4. net/socket.js
- **Current**: No tests.
- **Needed**:
  - Unit tests for Socket class: connection, reconnection, handler dispatch.
  - Test capped backoff and close/reconnect logic.
  - Mock WebSocket and verify message handling.

### 5. wrapped.js
- **Current**: No tests.
- **Needed**:
  - Unit tests for hideAndReveal and countUp logic.
  - Test DOM class toggling and animation behavior.

### 6. fireworks.js
- **Current**: No tests.
- **Needed**:
  - Unit tests for fireworksPressing/fireworksReleasing.
  - Test DOM class toggling and event listeners.

### 7. bootstrap/main.js
- **Current**: `frontend/src/__tests__/js/bootstrap/main.test.js` covers:
  - Preventing default `contextmenu` behavior.
  - Updating the press count DOM on `CurrentCount`.
  - Updating floating pressers on `PersonPressing` / `PersonReleased` (including the 100ms delayed update on release).
  - Wiring `pointerdown`/`pointerup` events to `socket.sendPressing()` / `socket.sendReleased()` and revealing the signup prompt after 16 presses.
  - Note: tests mock `Socket` and `renderFloatingPressers`.
- **Still needed**:
  - Higher-level integration coverage that uses the real `Socket` implementation (optional; unit tests already cover `net/socket.js`).
  - Coverage for missing `#pressMePls` behavior (currently assumed present).

## General Recommendations
- Add a test file for each module in `frontend/src/__tests__/`.
- Use jsdom for DOM-related tests.
- Mock WebSocket for socket tests.
- Ensure all pure logic and DOM wiring have at least basic coverage.

---
_Last updated: 2026-01-07_
