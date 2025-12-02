# WebSocket JSON Envelope Protocol Plan

Date: 2025-12-02T02:19:57.096Z
Status: Proposal (v2 protocol)

## Goals
- Replace ad-hoc frames ("pressing"/"released" and integer count) with a typed JSON envelope for both directions.
- Support multiple server-to-client message types (current count, person pressing, today's high, etc.) and future extensibility.
- Provide a safe migration path with backward compatibility and opt-in rollout.

## Envelope
- Shape (minimum):
  {
    "type": "<PascalCaseMessageName>",
    "body": { ... }
  }
- Optional extensions (future-friendly):
  - "version": 2 (integer protocol version; default 2 when omitted on v2 channel)
  - "requestId": "<string>" (correlate request/response, idempotency)
  - "ts": "<ISO-8601>" (producer timestamp)

Notes:
- Only "type" and "body" are required for v2.
- Message names are unique per direction; server and client may share some (e.g., Ping/Pong).

## Message taxonomy (initial set)

Client → Server
- PressStateChanged
  body: { "state": "pressing" | "released" }
- Ping
  body: { }
- Auth (optional future)
  body: { "token": "<jwt|opaque>" }

Server → Client
- CurrentCount
  body: { "count": <int> }
- PersonPressing
  body: { "id": "<string>", "displayName"?: "<string>" }
- TodaysHigh
  body: { "count": <int>, "at": "<ISO-8601>" }
- Snapshot (coalesced state)
  body: { "count": <int>, "pressingIds": ["<string>", ...], "todaysHigh": { "count": <int>, "at": "<ISO-8601>" } }
- Error
  body: { "code": "<string>", "message": "<string>", "details"?: object }
- Ack (when requestId is used)
  body: { "requestId": "<string>", "ok": true }
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
  - data class Envelope<T>(val type: String, val body: T, val version: Int? = null, val requestId: String? = null, val ts: Instant? = null)
  - sealed interface ClientMessage; sealed interface ServerMessage
  - data classes for each body type (PressStateChangedBody, CurrentCountBody, ...)
- Serialization
  - Prefer kotlinx.serialization with a custom polymorphic deserializer keyed by "type" to map to body class.
  - Alternatively, Jackson @JsonTypeInfo(use = Id.NAME, property = "type") with @JsonSubTypes.
- Controller
  - Decode incoming Text frames as Envelope<Any>, dispatch by type to handlers.
  - Produce Envelope<...> responses; centralize send via a small serializer helper.

## Backward compatibility and rollout
- Handshake opt-in: /ws?proto=2 (clients pass proto=2 to receive/send v2 envelopes).
- Server accepts both:
  - v1 inbound: raw strings "pressing"/"released" → map to PressStateChanged.
  - v1 outbound: if client did not opt-in, continue sending integer count frames only.
- Phases
  1) Add v2 support behind proto=2; keep v1 default.
  2) Update official clients to v2; monitor.
  3) Flip default to v2, keep v1 behind proto=1 for a deprecation window.
  4) Remove v1.

## Concurrency, flow control, and performance
- Maintain current coroutine/WS lifecycle; coalesce high-frequency updates (e.g., send Snapshot or CurrentCount on a throttle/debounce) to reduce fan-out.
- Use a cold-to-hot bridge (SharedFlow with replay=1) for state updates to avoid backpressure issues; drop oldest on overflow.
- Ensure send blocks are scoped and cancellable to avoid leaks on disconnect.

## Testing
- Unit tests: envelope encode/decode, unknown type handling, error mapping.
- Integration: WebSocketController end-to-end (v1 and v2), migration flag, throttling behavior.
- Load/soak: existing LoadTest updated to v2; verify throughput and memory.

## Observability
- Add counters by message type (in/out), deserialization failures, dropped sends, and throttle metrics.
- Log unknown "type" at warn with rate limiting; include requestId when present.

## Security
- Validate envelope sizes and body fields; cap message length.
- If Auth is used, require before accepting state-changing messages; tie to session.

## Open questions
- Do we need request/response correlation now (requestId) or later? Proposed: optional now, required for future RPC-like ops.
- Should server emit both CurrentCount and Snapshot, or only Snapshot after clients migrate? Proposed: keep both during transition.
