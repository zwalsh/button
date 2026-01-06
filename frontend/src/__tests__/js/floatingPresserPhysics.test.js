import { describe, it, expect } from 'vitest';
import { computeNextPillStates } from '../../main/js/floatingPresserPhysics.js';
import { makeMockPill } from './testUtils.js';

// Helper function to check positions within a tolerance
function expectPositionsCloseTo(pillStates, expectedPositions, tolerance = 5.1) {
  expect(pillStates.length).toBe(expectedPositions.length);
  for (let i = 0; i < pillStates.length; i++) {
    const pill = pillStates[i];
    const expected = expectedPositions[i];
    expect(Math.abs(pill.x - expected.x)).toBeLessThanOrEqual(tolerance);
    expect(Math.abs(pill.y - expected.y)).toBeLessThanOrEqual(tolerance);
  }

}
describe('computeNextPillStates position', () => {
  it('positions a single pill in the center', () => {
    const W = 400, H = 300;
    const pill = makeMockPill({ initialized: false });
    const next = computeNextPillStates([pill], { W, H, frame: 0 });
    expectPositionsCloseTo(next, [{ x: W / 2, y: H / 2 }]);
  });
  it('positions two pills symmetrically', () => {
    const W = 400, H = 300;
    const pillA = makeMockPill({ initialized: false });
    const pillB = makeMockPill({ initialized: false });
    const next = computeNextPillStates([pillA, pillB], { W, H, frame: 0 });
    const margin = 200;
    expectPositionsCloseTo(next, [
      { x: W / 2 + (W - margin) / 2, y: H / 2 },
      { x: W / 2 - (W - margin) / 2, y: H / 2 },
    ]);
  });
  it('positions N pills on oval', () => {
    const W = 400, H = 300, N = 5;
    const pills = Array.from({ length: N }, () => makeMockPill({ initialized: false }));
    const next = computeNextPillStates(pills, { W, H, frame: 0 });
    expectPositionsCloseTo(next, [
      { x: W / 2 + (W - 200) / 2, y: H / 2 },
      { x: W / 2 + ((W - 200) / 2) * Math.cos((2 * Math.PI) / 5), y: H / 2 + ((H - 40) / 2) * Math.sin((2 * Math.PI) / 5) },
      { x: W / 2 + ((W - 200) / 2) * Math.cos((4 * Math.PI) / 5), y: H / 2 + ((H - 40) / 2) * Math.sin((4 * Math.PI) / 5) },
      { x: W / 2 + ((W - 200) / 2) * Math.cos((6 * Math.PI) / 5), y: H / 2 + ((H - 40) / 2) * Math.sin((6 * Math.PI) / 5) },
      { x: W / 2 + ((W - 200) / 2) * Math.cos((8 * Math.PI) / 5), y: H / 2 + ((H - 40) / 2) * Math.sin((8 * Math.PI) / 5) },
    ]);
  });
});

describe('computeNextPillStates edge/collision/repel', () => {
  it('repels overlapping pills', () => {
    const W = 400, H = 300;
    const pillA = makeMockPill({ x: 200, y: 150 });
    const pillB = makeMockPill({ x: 210, y: 150 });
    const next = computeNextPillStates([pillA, pillB], { W, H, frame: 0 });
    expect(Math.abs(next[0].x - next[1].x)).toBeGreaterThan(Math.abs(pillA.x - pillB.x));
    expect(Math.abs(next[0].vx)).toBeGreaterThan(0);
    expect(Math.abs(next[1].vx)).toBeGreaterThan(0);
  });
  it('repels pills near edge', () => {
    const W = 400, H = 300;
    const pill = makeMockPill({ x: 10, y: 150 });
    const next = computeNextPillStates([pill], { W, H, frame: 0 });
    expect(next[0].x).toBeGreaterThan(pill.x);
  });
  it('applies soft repel for close pills', () => {
    const W = 400, H = 300;
    const pillA = makeMockPill({ x: 200, y: 150 });
    const pillB = makeMockPill({ x: 250, y: 150 });
    const next = computeNextPillStates([pillA, pillB], { W, H, frame: 0, OVAL_ATTRACT: 0 });
    expect(next[0].x).toBeLessThan(pillA.x);
    expect(next[1].x).toBeGreaterThan(pillB.x);
  });
});

describe('computeNextPillStates damping/frame', () => {
  it('applies base damping to velocity', () => {
    const W = 400, H = 300;
    const pill = makeMockPill({ x: 200, y: 150, vx: 10 });
    const next = computeNextPillStates([pill], { W, H, frame: 0, OVAL_ATTRACT: 0 });
    expect(next[0].vx).toBeCloseTo(10 * 0.94, 2);
  });
  it('ramps damping with frame', () => {
    const W = 400, H = 300;
    const pill = makeMockPill({ x: 200, y: 150, vx: 10 });
    const next = computeNextPillStates([pill], { W, H, frame: 100, OVAL_ATTRACT: 0 });
    const expectedDamping = Math.min(0.94 + 100 * 0.0012, 0.998);
    expect(next[0].vx).toBeCloseTo(10 * expectedDamping, 2);
  });
  it('caps damping at MAX_DAMPING', () => {
    const W = 400, H = 300;
    const pill = makeMockPill({ x: 200, y: 150, vx: 10 });
    const next = computeNextPillStates([pill], { W, H, frame: 1000, OVAL_ATTRACT: 0 });
    expect(next[0].vx).toBeCloseTo(10 * 0.998, 2);
  });
});
