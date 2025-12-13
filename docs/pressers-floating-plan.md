# Plan: Floating Name Pressers UI

_Last updated: 2025-12-13_

This plan describes how to implement a "floating name" UI for currently-pressing users, where each presser's name appears at a unique, consistent location on the screen, animating in/out as they press or release. The approach ensures no overlap with the main button, avoids name collisions, and is fully responsive.

---

## 1. Add Floating Name Container
- Add a `<div id="floating-pressers"></div>` to the main page HTML, positioned absolutely or fixed, covering the full viewport (z-index above background, below modals).
- Style with `pointer-events: none` so it doesn't block UI interactions.

## 2. Deterministic Positioning Algorithm
- Hash each presser name to a unique integer seed.
- Use the seed to generate a (x, y) position as a percentage of the viewport (0–100% for both axes).
- Exclude a central "no-fly zone" rectangle covering the main button (e.g., 30–70% x and 40–60% y), and add padding from edges.
- If a generated position falls in the no-fly zone, re-hash (e.g., add a salt or increment seed) until a valid spot is found.

## 3. Overlap Avoidance
- On each update, collect all current presser positions.
- For each new name, if its position is too close to an existing one (distance < threshold), nudge it outward in a spiral or random direction, or re-hash with a salt until a non-overlapping spot is found.
- Limit retries; if too crowded, allow minimal overlap or reduce font size for those pills.

## 4. Responsive Design
- Use viewport-relative units (vw/vh or %) for positions so pills move with window resizing.
- On resize, recalculate all positions and re-render.
- Ensure pills never overflow the viewport (adjust for pill width/height).

## 5. Rendering and Animation
- For each presser, render a `<span class="floating-presser-pill" style="left: X%; top: Y%;">Name</span>` inside the container.
- Animate entry (fade/slide in) and exit (fade/slide out) with CSS transitions.
- Truncate long names with ellipsis and show full name on hover (tooltip).

## 6. Integration
- In main.js, maintain the set of currently-pressing users.
- On PersonPressing/PersonReleased, update the set and re-render the floating pills.
- Use the same color-hashing logic for pill backgrounds as in the list approach.

## 7. Code Cleanup and Documentation
- Refactor position calculation and rendering logic for clarity.
- Document the algorithm and edge cases in code comments and docs.

---

## Optional Enhancements
- Animate pills to “wiggle” or pulse while pressing.
- Add collision-avoidance animation for new pills.
- Allow users to click a pill for more info (if pointer-events enabled).

---

## Rollout
- Implement in small steps: static container → deterministic positions → overlap avoidance → animation → responsive tweaks.
- Test with various screen sizes and many pressers.

---

This plan ensures a playful, visually appealing, and robust floating name UI that avoids overlap, respects the main button, and adapts responsively.
