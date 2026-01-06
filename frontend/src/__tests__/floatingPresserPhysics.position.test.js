import { describe, it, expect } from 'vitest';
import { computeNextPillStates } from '../main/js/floatingPresserPhysics.js';

function makeMockPill({ x, y, w = 40, h = 40, vx = 0, vy = 0, initialized = true } = {}) {
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

describe('computeNextPillStates', () => {
  it('positions a single pill in the center', () => {
    const W = 400, H = 300;
    const pill = makeMockPill({ initialized: false });
    const next = computeNextPillStates([pill], { W, H, frame: 0 });
    expect(Math.abs(next[0].x - W/2)).toBeLessThan(25);
    expect(Math.abs(next[0].y - H/2)).toBeLessThan(25);
  });

  it('positions two pills symmetrically', () => {
    const W = 400, H = 300;
    const pillA = makeMockPill({ initialized: false });
    const pillB = makeMockPill({ initialized: false });
    const next = computeNextPillStates([pillA, pillB], { W, H, frame: 0 });
    expect(next.length).toBe(2);
    // Pills should be on opposite sides of the oval
    expect(Math.abs(next[0].x - (W/2 + (W-200)/2))).toBeLessThan(50);
    expect(Math.abs(next[1].x - (W/2 - (W-200)/2))).toBeLessThan(50);
  });

  it('positions N pills on oval', () => {
    const W = 400, H = 300, N = 5;
    const pills = Array.from({ length: N }, () => makeMockPill({ initialized: false }));
    const next = computeNextPillStates(pills, { W, H, frame: 0 });
    expect(next.length).toBe(N);
    // All pills should be roughly on oval
    for (let i = 0; i < N; i++) {
      const dx = next[i].x - W/2;
      const dy = next[i].y - H/2;
      const rX = (W-200)/2, rY = (H-40)/2;
      // Check if pill is near oval
      expect(Math.abs(Math.sqrt((dx*dx)/(rX*rX) + (dy*dy)/(rY*rY)) - 1)).toBeLessThan(0.3);
    }
  });
});
