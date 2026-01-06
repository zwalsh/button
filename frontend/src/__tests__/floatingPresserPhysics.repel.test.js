import { describe, it, expect } from 'vitest';
import { computeNextPillStates } from '../main/js/floatingPresserPhysics.js';

import { makeMockPill } from './testUtils.js';

describe('computeNextPillStates edge/collision/repel', () => {
  it('repels overlapping pills', () => {
    const W = 400, H = 300;
    const pillA = makeMockPill({ x: 200, y: 150 });
    const pillB = makeMockPill({ x: 210, y: 150 }); // Overlapping X
    const next = computeNextPillStates([pillA, pillB], { W, H, frame: 0 });
    // Pills should move apart in X
    expect(Math.abs(next[0].x - next[1].x)).toBeGreaterThan(Math.abs(pillA.x - pillB.x));
    // Pills should have nonzero velocity in X (repel direction)
    expect(Math.abs(next[0].vx)).toBeGreaterThan(0);
    expect(Math.abs(next[1].vx)).toBeGreaterThan(0);
  });

  it('repels pills near edge', () => {
    const W = 400, H = 300;
    const pill = makeMockPill({ x: 10, y: 150 }); // Near left edge
    const next = computeNextPillStates([pill], { W, H, frame: 0 });
    // Should move right
    expect(next[0].x).toBeGreaterThan(pill.x);
  });

  it('applies soft repel for close pills', () => {
    const W = 400, H = 300;
    const pillA = makeMockPill({ x: 200, y: 150 });
    const pillB = makeMockPill({ x: 250, y: 150 }); // Within SOFT_REPEL_DIST
    // Disable oval attract to isolate soft repel
    const next = computeNextPillStates([pillA, pillB], { W, H, frame: 0, OVAL_ATTRACT: 0 });
    // Pills should move apart
    expect(next[0].x).toBeLessThan(pillA.x);
    expect(next[1].x).toBeGreaterThan(pillB.x);
  });
});
