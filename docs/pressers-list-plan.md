# Incremental Plan: Animated Pressers List UI

_Last updated: 2025-12-13_

This plan describes how to add a visually appealing, animated list of currently-pressing users ("pressers") as name pills along the bottom of the button app. The approach is incremental, minimizing risk and ensuring each step is testable.

---

## 1. Add Static Container
- Add a `<div id="pressers-list"></div>` to the main page HTML (in HomeController.kt) just above the closing `</body>` tag or at the bottom of the main content.
- Style the container in CSS to be bottom-aligned, full-width, and visually distinct (e.g., padding, background, shadow).
- Confirm it appears as expected (empty for now).

## 2. Basic Pill Rendering (No Animation)
- In main.js, maintain a JS array of currently-pressing users (with name and timestamp).
- On receiving PersonPressing/PersonReleased, update the array and re-render the pills in the container, sorted by most recent press first.
- Render each pill as a `<span class="pill presser-pill">Name</span>`, using existing .pill/.rounded styles and a new .presser-pill class for color.
- Truncate long names with CSS (`text-overflow: ellipsis; white-space: nowrap; overflow: hidden;`) and add a `title` attribute for tooltips.

## 3. Color and Visual Polish
- Hash each name to a background color for the pill (consistent per user).
- Adjust text color for contrast if needed.
- Refine pill spacing, font, and size for clarity and consistency with the app.

## 4. Multi-Row Layout and Truncation
- Use CSS flexbox or grid to allow pills to wrap to up to 3 rows.
- After 3 rows (or a set max pill count), stop rendering additional pills and add a "+N more" pill at the end.
- Test with many simulated pressers to ensure layout remains clean.

## 5. Animated Transitions
- Add CSS transitions for pill entry (fade/slide in), exit (fade/slide out), and reordering (smooth movement).
- On DOM updates, use classes or JS to trigger animations (e.g., add/remove classes, or use requestAnimationFrame for reordering effects).
- Test with rapid press/release events to ensure smoothness.

## 6. Code Cleanup and Documentation
- Refactor pill management code for clarity and maintainability.
- Document the new JS and CSS in code comments and update any relevant docs.

---

## Optional Enhancements
- Responsive tweaks for mobile (smaller pills, horizontal scroll if needed).
- Accessibility: ARIA labels, keyboard navigation for pills.
- Custom animation library (e.g., animate.css) if native CSS transitions are insufficient.

---

## Rollout
- Deploy each step incrementally, verifying UI and performance at each stage.
- Solicit feedback after each major step before proceeding.

---

This plan ensures a smooth, testable rollout of the animated pressers list, fully integrated with your current stack and style.
