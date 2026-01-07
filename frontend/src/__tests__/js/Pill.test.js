import { describe, it, expect, vi, beforeEach } from 'vitest';
import { Pill } from '../../main/js/Pill.js';

describe('Pill', () => {
  let domElement;
  beforeEach(() => {
    domElement = document.createElement('div');
    domElement.style.position = 'absolute';
    domElement.style.left = '10px';
    domElement.style.top = '20px';
    Object.defineProperty(domElement, 'offsetWidth', { value: 120, configurable: true });
    Object.defineProperty(domElement, 'offsetHeight', { value: 22, configurable: true });
  });

  it('initializes with default velocity and not initialized', () => {
    const pill = new Pill(domElement);
    expect(pill.vx).toBe(0);
    expect(pill.vy).toBe(0);
    expect(pill.isInitialized()).toBe(false);
  });

  it('returns correct position and size', () => {
    const pill = new Pill(domElement);
    expect(pill.leftX()).toBe(10);
    expect(pill.topY()).toBe(20);
    expect(pill.width()).toBe(120);
    expect(pill.height()).toBe(22);
    expect(pill.centerX()).toBe(10 + 60);
    expect(pill.centerY()).toBe(20 + 11);
    expect(pill.rightX()).toBe(10 + 120);
    expect(pill.bottomY()).toBe(20 + 22);
  });

  it('setCenter sets position safely and marks initialized', () => {
    const pill = new Pill(domElement);
    pill.setCenter(200, 100, 400, 300);
    expect(parseFloat(domElement.style.left)).toBeCloseTo(200 - 60, 1);
    expect(parseFloat(domElement.style.top)).toBeCloseTo(100 - 11, 1);
    expect(domElement.style.display).toBe('block');
    expect(pill.isInitialized()).toBe(true);
  });

  it('safeX and safeY clamp to container', () => {
    const pill = new Pill(domElement);
    // left edge
    expect(pill.safeX(0, 400)).toBe(60);
    // right edge
    expect(pill.safeX(400, 400)).toBe(400 - 60);
    // top edge
    expect(pill.safeY(0, 300)).toBe(11);
    // bottom edge
    expect(pill.safeY(300, 300)).toBe(300 - 11);
  });

  it('setVelocity sets vx and vy', () => {
    const pill = new Pill(domElement);
    pill.setVelocity(5, -3);
    expect(pill.vx).toBe(5);
    expect(pill.vy).toBe(-3);
  });

  it('createPill creates a pill with truncated name and correct class', () => {
    const pill = Pill.createPill('averyveryverylongusername');
    expect(pill.domElement.className).toBe('floating-presser-pill');
    expect(pill.domElement.textContent.endsWith('…')).toBe(true);
    expect(pill.domElement.style.position).toBe('absolute');
    expect(pill.domElement.style.display).toBe('none');
  });

  it('remove adds out class and removes from DOM after timeout', async () => {
    const pill = new Pill(domElement);
    document.body.appendChild(domElement);
    vi.useFakeTimers();
    pill.remove();
    expect(domElement.classList.contains('floating-presser-pill--out')).toBe(true);
    vi.advanceTimersByTime(200);
    await Promise.resolve(); // let microtasks run
    expect(domElement.parentNode).toBe(null);
    vi.useRealTimers();
  });
});
