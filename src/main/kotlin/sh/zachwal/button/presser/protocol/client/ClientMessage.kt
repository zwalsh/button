package sh.zachwal.button.presser.protocol.client

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(PressStateChanged::class)
)
/**
 * Base interface for all client-to-server protocol messages (e.g., PressStateChanged).
 *
 * Used for polymorphic serialization and message handling on the backend.
 */
sealed interface ClientMessage {
    @get:JsonIgnore
    val type: String get() = this::class.simpleName!!
}
