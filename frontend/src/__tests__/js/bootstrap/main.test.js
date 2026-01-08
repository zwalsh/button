import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

const socketInstances = [];

vi.mock('../../../main/js/net/socket.js', () => {
  return {
    default: class SocketMock {
      constructor({ url, handlers } = {}) {
        this.url = url;
        this.handlers = handlers;
        this.sendPressing = vi.fn();
        this.sendReleased = vi.fn();
        socketInstances.push(this);
      }
    },
  };
});

const renderFloatingPressers = vi.fn();
vi.mock('../../../main/js/floatingPresserPositions.js', () => {
  return { renderFloatingPressers };
});

describe('bootstrap/main', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.resetModules();

    socketInstances.length = 0;
    renderFloatingPressers.mockClear();

    document.body.innerHTML = '';
    Object.defineProperty(globalThis, 'wsUrl', {
      value: 'ws://example',
      configurable: true,
      writable: true,
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  async function loadModule() {
    await import('../../../main/js/bootstrap/main.js');
    expect(socketInstances.length).toBe(1);
    return socketInstances[0];
  }

  it('prevents default on contextmenu', async () => {
    await loadModule();

    const ev = new Event('contextmenu', { cancelable: true });
    document.dispatchEvent(ev);
    expect(ev.defaultPrevented).toBe(true);
  });

  it('updates press count DOM on CurrentCount messages', async () => {
    document.body.innerHTML = `
      <div id="buttonPressCount"></div>
      <div id="buttonPressCountWhite"></div>
    `;

    const socket = await loadModule();

    socket.handlers.onCurrentCount({ type: 'CurrentCount', count: 7 });

    expect(document.getElementById('buttonPressCount').innerText).toBe('BUTTON PRESSERS: 7');
    expect(document.getElementById('buttonPressCountWhite').innerText).toBe('BUTTON PRESSERS: 7');
  });

  it('does not throw if press count DOM nodes are missing', async () => {
    const socket = await loadModule();
    expect(() => socket.handlers.onCurrentCount({ type: 'CurrentCount', count: 1 })).not.toThrow();
  });

  it('tracks current pressers and renders on PersonPressing/PersonReleased', async () => {
    const socket = await loadModule();

    socket.handlers.onPersonPressing({ type: 'PersonPressing', displayName: 'alice' });
    socket.handlers.onPersonPressing({ type: 'PersonPressing', displayName: 'bob' });

    expect(renderFloatingPressers).toHaveBeenLastCalledWith(['alice', 'bob']);

    renderFloatingPressers.mockClear();

    socket.handlers.onPersonReleased({ type: 'PersonReleased', displayName: 'alice' });
    expect(renderFloatingPressers).not.toHaveBeenCalled();

    vi.advanceTimersByTime(100);
    expect(renderFloatingPressers).toHaveBeenCalledWith(['bob']);
  });

  it('wires pointer events to sendPressing/sendReleased and shows signup after 16 presses', async () => {
    document.body.innerHTML = `
      <button id="pressMePls"></button>
      <div id="signup" style="display:none"></div>
    `;

    const socket = await loadModule();

    // Install pointer handlers
    window.dispatchEvent(new Event('load'));

    const button = document.getElementById('pressMePls');
    const signup = document.getElementById('signup');

    for (let i = 0; i < 16; i++) {
      button.dispatchEvent(new Event('pointerdown'));
    }

    expect(socket.sendPressing).toHaveBeenCalledTimes(16);
    expect(signup.style.display).toBe('block');

    button.dispatchEvent(new Event('pointerup'));
    expect(socket.sendReleased).toHaveBeenCalledTimes(1);
  });
});
