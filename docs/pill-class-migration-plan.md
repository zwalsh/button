# Pill Class Migration Plan

**Date:** 2025-12-15T01:36:27.176Z

## Goal
Encapsulate pill DOM and physics logic in a Pill class to improve code sharing and maintainability between `floatingPresserPhysics.js` and `floatingPresserPositions.js`.

## Migration Plan

### 1. Identify All Access Points (Complete as of 2025-12-15)
- Located all code that reads/writes pill DOM element properties:
  - `style.left`, `style.top`, `offsetWidth`, `offsetHeight`, `dataset`
- Found all code that calculates or uses pill positions (center, left, top, etc.), velocity, and other physics-related properties:
  - `x`, `y`, `vx`, `vy`, `w`, `h`, and related calculations
- Found all code that creates, removes, or manages pill DOM elements:
  - `document.createElement`, `appendChild`, `removeChild`
- See floatingPresserPhysics.js and floatingPresserPositions.js for all access points.

### 2. Define Pill Class Interface
- Constructor accepts a DOM element reference.
- Expose methods:
  - `centerX()`, `centerY()`, `leftX()`, `topY()`
  - `setPosition(x, y)`
  - Get/set velocity, acceleration, size, dataset properties
- Optionally, static methods for creating/removing pill elements.

### 3. Implement Pill Class
- Encapsulate all DOM property accesses/mutations in methods.
- Store additional state (velocity, acceleration, etc.) as instance properties.
- Ensure all calculations (center, edges, etc.) are methods.

### 4. Refactor Usage
- Replace direct DOM accesses/mutations in both files with Pill class methods.
- Update pill creation to instantiate Pill objects.
- Update all callsites to use Pill instances and their methods.

### 5. Test and Validate
- Ensure all pill-related logic works as before.
- Confirm encapsulation and code sharing between physics and position logic.

---
This plan will guide the migration to a unified Pill class for improved maintainability and code reuse.
