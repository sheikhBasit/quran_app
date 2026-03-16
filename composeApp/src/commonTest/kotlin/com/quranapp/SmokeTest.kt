import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Smoke test — verifies commonTest is configured correctly.
 * This must pass before any feature work begins.
 */
class SmokeTest {
    @Test
    fun `environment is configured correctly`() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `kotlin test framework is available`() {
        val list = listOf(1, 2, 3)
        assertEquals(3, list.size)
    }
}
