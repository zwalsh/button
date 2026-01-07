import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import Socket from '../../../main/js/net/socket.js';

class FakeWebSocket {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;

  static instances = [];

  constructor(url) {
    this.url = url;
    this.readyState = FakeWebSocket.CONNECTING;

    this.onopen = null;
    this.onmessage = null;
    this.onclose = null;
    this.onerror = null;

    this.sent = [];
    this.closeCalls = 0;

    FakeWebSocket.instances.push(this);
  }

  open() {
    this.readyState = FakeWebSocket.OPEN;
    this.onopen?.({});
  }

  emitMessage(data) {
    const payload = typeof data === 'string' ? data : JSON.stringify(data);
    this.onmessage?.({ data: payload });
  }

  close() {
    this.closeCalls++;
    this.readyState = FakeWebSocket.CLOSED;
    this.onclose?.({});
  }

  send(data) {
    this.sent.push(data);
  }
}

describe('net/socket', () => {
  const realWebSocket = globalThis.WebSocket;

  beforeEach(() => {
    FakeWebSocket.instances = [];
    globalThis.WebSocket = FakeWebSocket;
    vi.useFakeTimers();

    vi.spyOn(console, 'debug').mockImplementation(() => {});
    vi.spyOn(console, 'info').mockImplementation(() => {});
    vi.spyOn(console, 'warn').mockImplementation(() => {});
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.useRealTimers();
    globalThis.WebSocket = realWebSocket;
    vi.restoreAllMocks();
  });

  it('requires a url', () => {
    expect(() => new Socket({ handlers: {} })).toThrow(/requires a url/i);
  });

  it('connects on construction and uses the provided url', () => {
    const socket = new Socket({ url: 'ws://example', handlers: {} });
    expect(FakeWebSocket.instances.length).toBe(1);
    expect(FakeWebSocket.instances[0].url).toBe('ws://example');
    socket.close();
  });

  it('resets backoff to initial on open', () => {
    const socket = new Socket({ url: 'ws://example', handlers: {} });
    socket.backoff = 5000;
    FakeWebSocket.instances[0].open();
    expect(socket.backoff).toBe(socket.BACKOFF_INITIAL);
    socket.close();
  });

  it('dispatches CurrentCount/PersonPressing/PersonReleased messages to handlers', () => {
    const handlers = {
      onCurrentCount: vi.fn(),
      onPersonPressing: vi.fn(),
      onPersonReleased: vi.fn(),
    };

    const socket = new Socket({ url: 'ws://example', handlers });
    const ws = FakeWebSocket.instances[0];

    ws.emitMessage({ type: 'CurrentCount', count: 3 });
    ws.emitMessage({ type: 'PersonPressing', name: 'alice' });
    ws.emitMessage({ type: 'PersonReleased', name: 'bob' });

    expect(handlers.onCurrentCount).toHaveBeenCalledWith({ type: 'CurrentCount', count: 3 });
    expect(handlers.onPersonPressing).toHaveBeenCalledWith({ type: 'PersonPressing', name: 'alice' });
    expect(handlers.onPersonReleased).toHaveBeenCalledWith({ type: 'PersonReleased', name: 'bob' });

    socket.close();
  });

  it('ignores bad JSON from server', () => {
    const handlers = {
      onCurrentCount: vi.fn(),
      onPersonPressing: vi.fn(),
      onPersonReleased: vi.fn(),
    };

    const socket = new Socket({ url: 'ws://example', handlers });
    const ws = FakeWebSocket.instances[0];

    ws.emitMessage('{ this is not json');

    expect(handlers.onCurrentCount).not.toHaveBeenCalled();
    expect(handlers.onPersonPressing).not.toHaveBeenCalled();
    expect(handlers.onPersonReleased).not.toHaveBeenCalled();

    socket.close();
  });

  it('drops outbound messages unless the socket is OPEN', () => {
    const socket = new Socket({ url: 'ws://example', handlers: {} });
    const ws = FakeWebSocket.instances[0];

    socket.sendPressing();
    socket.sendReleased();
    expect(ws.sent.length).toBe(0);

    ws.open();
    socket.sendPressing();
    socket.sendReleased();

    expect(ws.sent).toEqual([
      JSON.stringify({ type: 'PressStateChanged', state: 'PRESSING' }),
      JSON.stringify({ type: 'PressStateChanged', state: 'RELEASED' }),
    ]);

    socket.close();
  });

  it('reconnects on close with capped exponential backoff', () => {
    const socket = new Socket({ url: 'ws://example', handlers: {} });
    const ws1 = FakeWebSocket.instances[0];

    socket.backoff = 25000;
    ws1.close();

    expect(vi.getTimerCount()).toBe(1);
    vi.advanceTimersByTime(25000);

    expect(FakeWebSocket.instances.length).toBe(2);
    expect(socket.backoff).toBe(30000); // capped

    const ws2 = FakeWebSocket.instances[1];
    ws2.close();
    vi.advanceTimersByTime(30000);

    expect(FakeWebSocket.instances.length).toBe(3);
    expect(socket.backoff).toBe(30000); // stays capped

    socket.close();
  });

  it('does not schedule multiple reconnect timers', () => {
    const socket = new Socket({ url: 'ws://example', handlers: {} });
    const ws = FakeWebSocket.instances[0];

    ws.close();
    ws.close();

    expect(vi.getTimerCount()).toBe(1);
    socket.close();
  });

  it('close() stops reconnect attempts', () => {
    const socket = new Socket({ url: 'ws://example', handlers: {} });
    const ws = FakeWebSocket.instances[0];

    socket.close();
    ws.close();
    vi.runOnlyPendingTimers();

    expect(FakeWebSocket.instances.length).toBe(1);
  });
});
