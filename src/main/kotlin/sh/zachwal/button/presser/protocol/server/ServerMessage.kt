package sh.zachwal.button.presser.protocol.server

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(CurrentCount::class)
)
sealed interface ServerMessage {
    val type: String get() = this::class.simpleName!!
    val ts: Instant
}