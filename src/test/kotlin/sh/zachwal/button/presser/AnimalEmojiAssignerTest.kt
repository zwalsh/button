package sh.zachwal.button.presser

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class AnimalEmojiAssignerTest {

    @Test
    fun `next returns the first animal emoji on the first call`() {
        val assigner = AnimalEmojiAssigner()

        assertThat(assigner.next()).isEqualTo(AnimalEmojiAssigner.ANIMAL_EMOJIS[0])
    }

    @Test
    fun `next returns emoji in sequence on successive calls`() {
        val assigner = AnimalEmojiAssigner()

        val emojis = AnimalEmojiAssigner.ANIMAL_EMOJIS
        assertThat(assigner.next()).isEqualTo(emojis[0])
        assertThat(assigner.next()).isEqualTo(emojis[1])
        assertThat(assigner.next()).isEqualTo(emojis[2])
    }

    @Test
    fun `next loops back to the beginning after the sequence is exhausted`() {
        val assigner = AnimalEmojiAssigner()
        val emojis = AnimalEmojiAssigner.ANIMAL_EMOJIS

        repeat(emojis.size) { assigner.next() }

        assertThat(assigner.next()).isEqualTo(emojis[0])
    }
}
