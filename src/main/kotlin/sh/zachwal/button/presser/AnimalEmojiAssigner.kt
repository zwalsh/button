package sh.zachwal.button.presser

import com.google.inject.Singleton
import java.util.concurrent.atomic.AtomicInteger

/**
 * Hands out animal emoji names to anonymous pressers from a centrally managed sequence.
 *
 * Emoji are handed out in order and loop back to the beginning once the list is exhausted, so
 * that concurrently connected anonymous pressers are unlikely to share the same emoji.
 */
@Singleton
class AnimalEmojiAssigner {

    companion object {
        val ANIMAL_EMOJIS = listOf(
            "🐱", "🐶", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷",
            "🐸", "🐵", "🐔", "🐧", "🐦", "🦉", "🦄", "🐝", "🐢", "🐙",
        )
    }

    private val index = AtomicInteger(0)

    fun next(): String {
        val i = index.getAndUpdate { (it + 1) % ANIMAL_EMOJIS.size }
        return ANIMAL_EMOJIS[i]
    }
}
