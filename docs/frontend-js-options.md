# Frontend JS: ES Modules Plan

Date: 2025-12-28T17:24:07.312Z

Summary
- Adopt native ES modules (type="module") with vanilla JS and SSR-served HTML.
- Use explicit imports in every module (do not rely on script load order).
- Keep the current inline-injected wsUrl global for PR1.
- Use Vitest + jsdom for tests and add npm test; integrate it into CI (Jenkinsfile).
- Migrate admin pages to the ESM bootstrap like other pages.

Goals
- Explicit dependencies, no globals (except the temporary wsUrl shim).
- Separate pure logic (unit-testable) from DOM wiring.
- Add focused unit and small DOM integration tests.

Directory layout (recommended)
```
src/main/resources/static/src/js/
  net/socket.js                 # WebSocket client with capped backoff
  features/floating-pressers/
    physics.js                  # Pure computeNextPillStates(pills, config)
    pill.js                     # Pill class, DOM helpers
    positions.js                # renderFloatingPressers(names), owns RAF loop
  features/fireworks.js         # initFireworks(buttonEl, targets)
  features/wrapped.js           # initWrapped(buttons)
  bootstrap/main.js             # Page-specific wiring (DOM + socket + features)
```

Module responsibilities
- physics.js: pure functions, no DOM.
- pill.js: export class Pill; minimal DOM helpers.
- positions.js: imports pill + physics; exports renderFloatingPressers(names) and runs animation loop.
- fireworks.js / wrapped.js: export init functions that accept DOM elements.
- net/socket.js: export createSocket({ url, handlers }) and send(json); handle reconnect centrally with capped backoff.
- bootstrap/main.js: resolve DOM, attach single contextmenu prevention, create socket with inline wsUrl, wire feature inits.

Implementation plan (phased)

PR1 — ESM bootstrap and minimal wiring
- Add bootstrap/main.js that runs on DOMContentLoaded, uses explicit imports, and wires features.
- Change SSR script tags to type="module" and include modules in any page that needs them.
- Keep inline wsUrl global for PR1 (no change).

PR2 — Moduleize floating pressers
- Move physics, pill, and positions into exported modules; stop writing to window.

PR3 — Socket extraction
- Implement net/socket.js with createSocket and capped backoff reconnect; remove reconnect-on-send behavior.
- Simplify main.js to maintain currentPressers and call renderFloatingPressers(names) when the set changes.

PR4 — Moduleize other features
- Export initFireworks and initWrapped and call from bootstrap; consolidate contextmenu prevention.

PR5 — Tests and CI
- Add package.json with vitest + jsdom devDependencies and a "test" script.
- Write unit tests for physics, pill, positions, socket, fireworks, wrapped.
- Run npm ci && npm test in Jenkinsfile so frontend tests run in CI.

Testing choice
- Use Vitest over Jest: Vitest is faster, built for native ESM, simpler to configure, and pairs well with modern toolchains; jsdom provides DOM for integration tests.

Acceptance criteria
- Modules use import/export; no runtime window exports (except temporary wsUrl).
- One bootstrap controls event wiring and removes duplicate listeners.
- WebSocket reconnect uses capped backoff and is managed only in net/socket.js.
- Floating presser behavior is unchanged to users.
- Frontend unit tests exist and run in CI via npm test.

Risks & mitigations
- SSR script order errors: modules should import what they need; bootstrap guarded against missing DOM elements.
- Reconnect loops: cap backoff and stop after N attempts; log transitions.
- Animation regressions: physics logic stays pure and covered by unit tests.

Environment notes
- Ktor serves static files unchanged. Target modern mobile browsers (iOS Safari/Chrome). Use native ESM (no bundling).
- wsUrl is injected inline by HomeController; PR1 keeps this mechanism.
- Admin pages currently include jQuery; plan is to migrate them to use the ESM bootstrap instead of classic script tags.

Progress (2025-12-28T17:50:28.607Z)
- Converted Pill, floatingPresserPhysics, floatingPresserPositions, fireworks, and wrapped to ES modules (exported symbols; removed window/module.exports usage).
- Updated HomeController and WrappedController to emit script tags with type="module" and renamed main.js to bootstrap/main.js.
- Added bootstrap/main.js that preserves current WebSocket wiring and uses the inline wsUrl shim to maintain existing runtime behavior.

Remaining tasks
- Implement net/socket.js with a capped-backoff reconnect strategy and centralize WebSocket logic (PR2/PR3).
- Import and initialize fireworks and wrapped features from bootstrap/main.js (PR4).
- Add package.json with Vitest/jsdom, write frontend unit tests, and run npm test in CI (PR5).
