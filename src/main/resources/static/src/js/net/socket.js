// net/socket.js
// OOP-style WebSocket client for button app
// Exports: Socket class (default export)
// Handlers supported: onCurrentCount(msg), onPersonPressing(msg), onPersonReleased(msg)
// Instance methods: sendPressing(), sendReleased(), close()
// Behavior:
// - Automatically connects to provided url on construction.
// - Reconnects on close using internal capped exponential backoff.
// - Dispatches parsed server messages for CurrentCount, PersonPressing, PersonReleased to handlers.
// - Outbound press messages are JSON-stringified and sent only when the socket is OPEN; otherwise they are dropped.
// Date: 2026-01-03T16:01:14.724Z

class Socket {
  constructor({ url, handlers } = {}) {
    if (!url) throw new Error('Socket requires a url');
    this.url = url;
    this.onCurrentCount =  handlers.onCurrentCount || function() {};
    this.onPersonPressing = handlers.onPersonPressing || function() {};
    this.onPersonReleased = handlers.onPersonReleased || function() {};

    this.BACKOFF_INITIAL = 1000;
    this.BACKOFF_FACTOR = 1.5;
    this.BACKOFF_MAX = 30000;

    this.ws = null;
    this.backoff = this.BACKOFF_INITIAL;
    this.reconnectTimer = null;
    this._closed = false; // tracks if close() was called to stop reconnects

    this.connect();
  }

  connect() {
    if (this._closed) return;
    this.ws = new WebSocket(this.url);

    this.ws.onopen = () => {
      // reset backoff after a successful open
      this.backoff = this.BACKOFF_INITIAL;
    };

    this.ws.onmessage = (ev) => this._handleMessage(ev);

    this.ws.onclose = () => this._scheduleReconnect();

    this.ws.onerror = () => {
      // no-op; onclose will drive reconnect
    };
  }

  _handleMessage(ev) {
    let msg;
    try {
      msg = JSON.parse(ev.data);
    } catch (e) {
      console.error('Bad JSON from server: ' + ev.data);
      return;
    }

    switch (msg.type) {
      case 'CurrentCount':
        this.onCurrentCount(msg);
        break;
      case 'PersonPressing':
        this.onPersonPressing(msg);
        break;
      case 'PersonReleased':
        this.onPersonReleased(msg);
        break;
      default:
        console.error('Unknown message type received from server: ', msg.type);
        break;
    }
  }

  _scheduleReconnect() {
    if (this._closed) return;
    if (this.reconnectTimer) return;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
      this.backoff = Math.min(this.BACKOFF_MAX, Math.floor(this.backoff * this.BACKOFF_FACTOR));
    }, this.backoff);
  }

  _sendRaw(obj) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      try {
        this.ws.send(JSON.stringify(obj));
      } catch (e) {
        // swallow
      }
    }
    // dropped when not open
  }

  sendPressing() {
    this._sendRaw({ type: 'PressStateChanged', state: 'PRESSING' });
  }

  sendReleased() {
    this._sendRaw({ type: 'PressStateChanged', state: 'RELEASED' });
  }

  close() {
    // stop reconnect attempts and close socket
    this._closed = true;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.ws) {
      try {
        this.ws.close();
      } catch (e) {
        // ignore
      }
    }
  }
}

export default Socket;
