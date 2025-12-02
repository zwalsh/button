package sh.zachwal.button.guice

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton

class JacksonModule : AbstractModule() {

    @Provides
    @Singleton
    fun objectMapper() = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
}