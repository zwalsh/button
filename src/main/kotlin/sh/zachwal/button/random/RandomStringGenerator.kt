package sh.zachwal.button.random

import java.security.SecureRandom
import kotlin.streams.toList

class RandomStringGenerator {

    private val secureRandom = SecureRandom()
    private val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

    fun newToken(length: Long): String {
        return secureRandom.ints(length, 0, chars.length).toList()
            .map { randInt ->
                chars[randInt]
            }.fold(StringBuilder()) { sb, char ->
                sb.append(char)
            }.toString()
    }
}
