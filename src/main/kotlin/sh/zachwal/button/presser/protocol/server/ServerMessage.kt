package sh.zachwal.button.presser.protocol.server

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(CurrentCount::class),
    JsonSubTypes.Type(PersonPressing::class),
    JsonSubTypes.Type(PersonReleased::class)
)
/**
 * Base interface for all server-to-client protocol messages (e.g., CurrentCount, PersonPressing).
 *
 * Used for polymorphic serialization and message handling on the frontend.
 */
sealed interface ServerMessage {
    @get:JsonIgnore
    val type: String get() = this::class.simpleName!!
}
