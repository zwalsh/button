import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderFloatingPressers, assignNames } from '../main/js/floatingPresserPositions.js';

// Mock Pill and computeNextPillStates
const mockDom = () => {
  document.body.innerHTML = `
    <div id="floating-pressers-top"></div>
    <div id="floating-pressers-bottom"></div>
  `;
};

global.Pill = {
  createPill: vi.fn((name) => {
    const el = document.createElement('div');
    el.className = 'floating-presser-pill';
    el.textContent = name;
    el.style.position = 'absolute';
    el.style.display = '';
    return {
      name,
      domElement: el,
      setCenter: vi.fn(),
      setVelocity: vi.fn(),
      remove: vi.fn(function() {
        if (el.parentNode) {
          el.parentNode.removeChild(el);
        }
      }),
    };
  })
};

global.computeNextPillStates = vi.fn((pills, { W, H, frame }) => {
  return pills.map((_, i) => ({ x: i * 10, y: i * 5, vx: 0, vy: 0 }));
});

describe('floatingPresserPositions', () => {
  beforeEach(async () => {
    mockDom();
    // Reset Pill.createPill mocks
    global.Pill.createPill.mockClear();
    // Reset pillState and nameAssignments between tests
    const mod = await import('../main/js/floatingPresserPositions.js');
    if (mod.__resetTestState) mod.__resetTestState();
});

  it('creates pills for each name and adds to DOM', () => {
    renderFloatingPressers(['alice', 'bob', 'carol']);
    const topDiv = document.getElementById('floating-pressers-top');
    const botDiv = document.getElementById('floating-pressers-bottom');
    // First 5 go to bottom
    expect(botDiv.children.length).toBe(3);
    expect(topDiv.children.length).toBe(0);
  });

  it('removes pills when names disappear', async () => {
    renderFloatingPressers(['alice', 'bob']);
    renderFloatingPressers(['alice']);
    const botDiv = document.getElementById('floating-pressers-bottom');
    await new Promise(r => setTimeout(r, 210));
    expect(botDiv.children.length).toBe(1);
  });

  it('does nothing if required DOM nodes are missing', () => {
    document.body.innerHTML = '';
    expect(() => renderFloatingPressers(['x'])).not.toThrow();
  });

  it('does not duplicate pills in DOM', () => {
    renderFloatingPressers(['alice']);
    renderFloatingPressers(['alice']);
    const botDiv = document.getElementById('floating-pressers-bottom');
    expect(botDiv.children.length).toBe(1);
  });

   it("assigns first 5 names to bottom", () => {
     const { topNames, bottomNames } = assignNames(["a", "b", "c", "d", "e"]);
     expect(bottomNames).toEqual(["a", "b", "c", "d", "e"]);
     expect(topNames).toEqual([]);
   });

   it("assigns next 5 names to top", () => {
     const { topNames, bottomNames } = assignNames([
       "a",
       "b",
       "c",
       "d",
       "e",
       "f",
       "g",
       "h",
       "i",
       "j",
     ]);
     expect(bottomNames).toEqual(["a", "b", "c", "d", "e"]);
     expect(topNames).toEqual(["f", "g", "h", "i", "j"]);
   });

   it("alternates names after 10", () => {
     const names = Array.from({ length: 14 }, (_, i) =>
       String.fromCharCode(97 + i)
     ); // a-n
     const { topNames, bottomNames } = assignNames(names);
     expect(bottomNames).toEqual(["a", "b", "c", "d", "e", "k", "m"]);
     expect(topNames).toEqual(["f", "g", "h", "i", "j", "l", "n"]);
   });

   it("removes missing names and opens slots", () => {
     assignNames(["a", "b", "c", "d", "e"]);
     const { topNames, bottomNames } = assignNames(["a", "b", "c", "d"]);
     expect(bottomNames).toEqual(["a", "b", "c", "d"]);
     expect(topNames).toEqual([]);
   });

   it("fills open slots with new names", () => {
     assignNames(["a", "b", "c", "d", "e"]);
     assignNames(["a", "b", "c", "d"]);
     const { topNames, bottomNames } = assignNames(["a", "b", "c", "d", "x"]);
     expect(bottomNames).toEqual(["a", "b", "c", "d", "x"]);
     expect(topNames).toEqual([]);
   });

   it("keeps top slot names in top slot when count drops from 6 to 5", () => {
     assignNames(["a", "b", "c", "d", "e", "f"]);
     // f should be in topNames
     let result = assignNames(["a", "b", "c", "d", "e", "f"]);
     expect(result.bottomNames).toEqual(["a", "b", "c", "d", "e"]);
     expect(result.topNames).toEqual(["f"]);
     // Remove e -- f stays in top slots
     result = assignNames(["a", "b", "c", "d", "f"]);
     expect(result.bottomNames).toEqual(["a", "b", "c", "d"]);
     expect(result.topNames).toEqual(["f"]);
     result = assignNames(["a", "b", "c", "d", "f", "g"]);
     expect(result.bottomNames).toEqual(["a", "b", "c", "d", "g"]);
     expect(result.topNames).toEqual(["f"]);
   });
});
