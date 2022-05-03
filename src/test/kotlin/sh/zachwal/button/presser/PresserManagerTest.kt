package sh.zachwal.button.presser

import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test

internal class PresserManagerTest {

    @Test
    fun concurrentOperationsTest() {
        val pm = PresserManager()
        val pressers = (1..100).map { mockk<Presser>(relaxed = true) }
        runBlocking {
            pressers.forEach {
                pm.addPresser(it)
            }

            withContext(newFixedThreadPoolContext(4, "test")) {
                async {
                    repeat(100) {
                        pm.pressed(pressers.random())
                    }
                }
                async {
                    repeat(100) {
                        pm.released(pressers.random())
                    }
                }
                async {
                    repeat(100) {
                        pm.disconnected(pressers.random())
                    }
                }
                async {
                    repeat(100) {
                        pm.addPresser(pressers.random())
                    }
                }
            }
        }
    }
}
