package sh.zachwal.button.presser.protocol.client

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(PressStateChanged::class)
)
sealed interface ClientMessage {
    @get:JsonIgnore
    val type: String get() = this::class.simpleName!!
    val ts: Instant
}