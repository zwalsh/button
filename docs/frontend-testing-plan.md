# Frontend Testing Plan

## Overview
The frontend codebase is now modularized with ES modules and uses Vitest + jsdom for testing. Current test coverage is minimal, with only a basic test for `computeNextPillStates` in `floatingPresserPhysics.js`. The following plan breaks down coverage by module and identifies areas needing additional tests.

## Current Coverage
- **floatingPresserPhysics.js**: Has a basic test in `__tests__/floatingPresserPhysics.test.js` for empty input.
- **Other modules**: No dedicated test files or coverage found for `Pill.js`, `floatingPresserPositions.js`, `net/socket.js`, `wrapped.js`, `fireworks.js`, or `bootstrap/main.js`.

## Coverage Plan by Module

### 1. floatingPresserPhysics.js
- **Current**: Minimal test for empty input.
- **Needed**:
  - Test pill positioning logic for 1, 2, and N pills.
  - Test edge/collision/repel logic and config options.
  - Test damping and animation frame calculations.

### 2. Pill.js
- **Current**: No tests.
- **Needed**:
  - Unit tests for Pill class methods (position, velocity, DOM interaction).
  - Test initialization and state transitions.

### 3. floatingPresserPositions.js
- **Current**: No tests.
- **Needed**:
  - Test pill creation/removal logic (getOrCreatePill, cleanupPills).
  - Test renderFloatingPressers with various name sets.
  - Integration tests with DOM (using jsdom).

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
- **Current**: No tests.
- **Needed**:
  - Integration tests for event wiring and socket interaction.
  - Test that UI updates on socket events.

## General Recommendations
- Add a test file for each module in `frontend/src/__tests__/`.
- Use jsdom for DOM-related tests.
- Mock WebSocket for socket tests.
- Ensure all pure logic and DOM wiring have at least basic coverage.

---
_Last updated: 2026-01-04_
