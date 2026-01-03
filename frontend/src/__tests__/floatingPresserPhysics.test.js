import { describe, it, expect } from 'vitest';
import { computeNextPillStates } from '../main/js/floatingPresserPhysics.js';

describe('computeNextPillStates', () => {
  it('returns an array for empty input', () => {
    const next = computeNextPillStates([], { dt: 0.016 });
    expect(Array.isArray(next)).toBe(true);
  });
});
