import { describe, it, expect } from 'vitest';
import { computeNextPillStates } from '../main/js/floatingPresserPhysics.js';

function makeMockPill({ x, y, vx = 10, vy = 0, w = 40, h = 40, initialized = true } = {}) {
  let _x = x ?? 0, _y = y ?? 0;
  return {
    vx, vy,
    width: () => w,
    height: () => h,
    centerX: () => _x,
    centerY: () => _y,
    leftX: () => _x - w/2,
    rightX: () => _x + w/2,
    topY: () => _y - h/2,
    bottomY: () => _y + h/2,
    isInitialized: () => initialized,
    setCenter: (nx, ny) => { _x = nx; _y = ny; },
    x: _x,
    y: _y
  };
}

describe('computeNextPillStates damping/frame', () => {
  it('applies base damping to velocity', () => {
    const W = 400, H = 300;
    const pill = makeMockPill({ x: 200, y: 150, vx: 10 });
    const next = computeNextPillStates([pill], { W, H, frame: 0, OVAL_ATTRACT: 0 });
    // Velocity should be reduced by BASE_DAMPING
    expect(next[0].vx).toBeCloseTo(10 * 0.94, 2);
  });

  it('ramps damping with frame', () => {
    const W = 400, H = 300;
    const pill = makeMockPill({ x: 200, y: 150, vx: 10 });
    const next = computeNextPillStates([pill], { W, H, frame: 100, OVAL_ATTRACT: 0 });
    // Damping should be BASE_DAMPING + frame * DAMPING_RAMP
    const expectedDamping = Math.min(0.94 + 100 * 0.0012, 0.998);
    expect(next[0].vx).toBeCloseTo(10 * expectedDamping, 2);
  });

  it('caps damping at MAX_DAMPING', () => {
    const W = 400, H = 300;
    const pill = makeMockPill({ x: 200, y: 150, vx: 10 });
    const next = computeNextPillStates([pill], { W, H, frame: 1000, OVAL_ATTRACT: 0 });
    // Damping should not exceed MAX_DAMPING
    expect(next[0].vx).toBeCloseTo(10 * 0.998, 2);
  });
});
