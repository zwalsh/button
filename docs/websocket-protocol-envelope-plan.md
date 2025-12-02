# WebSocket JSON Envelope Protocol Plan

Date: 2025-12-02T02:35:33.502Z
Status: Proposal (v2 protocol)

## Goals
- Replace ad-hoc frames ("pressing"/"released" and integer count) with a typed JSON envelope for both directions.
- Support multiple server-to-client message types (current count, person pressing, today's high, etc.) and future extensibility.
- Provide a safe migration path with backward compatibility and opt-in rollout.

## Envelope
- Shape (minimum):
  {
    "type": "<PascalCaseMessageName>",
    ...
  }
- Optional extensions (future-friendly):
  - "requestId": "<string>" (correlate request/response, idempotency)
  - "ts": "<ISO-8601>" (producer timestamp)

Notes:
- Only "type" and "ts" are required.
- Message names are unique per direction; server and client may share some (e.g., Ping/Pong).

## Message taxonomy (initial set)

Client → Server
- PressStateChanged
  body: { "state": "pressing" | "released" }
- Ping
  body: { }

Server → Client
- CurrentCount
  body: { "count": <int> }
- PersonPressing
  body: { "displayName": "<string>" }
- TodaysHigh
  body: { "count": <int>, "at": "<ISO-8601>" }
- Snapshot (coalesced state)
  body: { "count": <int>, "pressingNames": ["<string>", ...], "todaysHigh": { "count": <int> } }
- Pong
  body: { }

## Examples
- Client → Server (pressing):
  { "type": "PressStateChanged", "body": { "state": "pressing" } }
- Client → Server (released):
  { "type": "PressStateChanged", "body": { "state": "released" } }
- Server → Client (current count):
  { "type": "CurrentCount", "body": { "count": 123 } }
- Server → Client (today's high):
  { "type": "TodaysHigh", "body": { "count": 456, "at": "2025-12-02T01:23:45Z" } }

## Kotlin implementation sketch
- Data model
  - data class ServerEnvelope<T : ServerMessage>(val type: String, val body: T, val ts: Instant? = null)
  - data class ClientEnvelope<T : ClientMessage>(val type: String, val body: T, val ts: Instant? = null)
  - sealed interface ClientMessage; sealed interface ServerMessage
  - data classes for each body type (PressStateChangedBody, CurrentCountBody, ...)
- Serialization
  - Jackson @JsonTypeInfo(use = Id.NAME, property = "type") with @JsonSubTypes.
- Controller
  - Decode incoming Text frames as ClientEnvelope<Any>, dispatch by type to handlers.
  - Produce ServerEnvelope<...> responses; centralize send via a small serializer helper.

## Backward compatibility and rollout
- None. Volume is low, we can directly update to the new version.

## Concurrency, flow control, and performance
- Maintain current coroutine/WS lifecycle; coalesce high-frequency updates (e.g., send Snapshot or CurrentCount on a throttle/debounce) to reduce fan-out.
- Use a cold-to-hot bridge (SharedFlow with replay=1) for state updates to avoid backpressure issues; drop oldest on overflow.
- Ensure send blocks are scoped and cancellable to avoid leaks on disconnect.

## Testing
- Unit tests: envelope encode/decode, unknown type handling, error mapping.
- Integration: WebSocketController end-to-end, throttling behavior.
- Load/soak: existing LoadTest updated; verify throughput and memory.

## Observability
- Add counters by message type (in/out), deserialization failures, dropped sends, and throttle metrics.
- Log unknown "type" at error with rate limiting; include requestId when present.

## Security
- Validate envelope sizes and body fields; cap message length.

## Open questions
- Do we need request/response correlation now (requestId) or later? Proposed: optional now, required for future RPC-like ops.

## Frontend adaptation (minimal JS)
- Replace legacy string protocol entirely; frontend now only sends/receives JSON envelopes.
- Receiving:
  - socket.onmessage: JSON.parse(event.data) (wrapped in try/catch). If parsing fails, log and return.
  - Switch on msg.type:
    - CurrentCount: update count display elements (#buttonPressCount, #buttonPressCountWhite).
    - Snapshot: update count, pressing list (#pressersList if present), today's high (#todaysHigh if present).
    - TodaysHigh: update high display.
    - PersonPressing: optional visual feedback (flash name, future enhancement).
    - Pong: ignore.
  - Unknown type: log (rate limited) and ignore.
- Sending:
  - pressing(): socket.send(JSON.stringify({ type: "PressStateChanged", body: { state: "pressing" } }))
  - released(): socket.send(JSON.stringify({ type: "PressStateChanged", body: { state: "released" } }))
  - Optionally factor sendPressState(state) helper.
- HTML/CSS:
  - Keep purely static lightweight markup. Add optional elements (#todaysHigh, #pressersList) guarded by existence checks before updating.
- Resilience:
  - Wrap JSON.parse in try/catch; ignore messages > 4KB.
- Example updated onmessage:
  ```js
  socket.onmessage = (event) => {
    let msg; try { msg = JSON.parse(event.data); } catch { console.warn('Bad JSON'); return; }
    switch (msg.type) {
      case 'CurrentCount': updateCount(msg.body.count); break;
      case 'Snapshot': updateSnapshot(msg.body); break;
      case 'TodaysHigh': updateHigh(msg.body); break;
      default: /* ignore */ break;
    }
  };
  ```
