import bob.free.add
import bob.free.get
import bob.free.getOrElse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class SingletonsKtTest {
    @Test
    fun `test string`() {
        add { "foobar" }
        assertTrue(get<String>().exists { it == "foobar" })
    }

    @Test
    fun `test string depend on int`() {
        add { "hello ${get<Int>().getOrElse { null }}" }
        add { 1 }
        assertTrue(get<String>().exists { it == "hello 1" })
    }

    @Test
    fun `test not exist`() {
        assertTrue(get<Boolean>().isFailure())
    }

    @Test
    fun `test lazy`() {
        val ref = AtomicInteger(0)
        add { ref.incrementAndGet() }
        assertEquals(0, ref.get())
        assertTrue(get<Int>().exists { it == 1 })
    }

    @Test
    fun `test oneshot`() {
        val ref = AtomicInteger(0)
        add { ref.incrementAndGet() }
        assertTrue(get<Int>().exists { it == 1 })
        assertTrue(get<Int>().exists { it == 1 })
    }

    @Test
    fun `test null`() {
        add { null as String }
        assertTrue(get<String>().isFailure())
    }

    @Test
    fun `test exception`() {
        add<String> { throw RuntimeException() }
        assertTrue(get<String>().isFailure())
    }
}