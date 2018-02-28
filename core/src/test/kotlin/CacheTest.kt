package bob.free
import com.nhaarman.mockito_kotlin.clearInvocations
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.junit.Assert.*
import org.junit.Test

class CacheTest {
    @Test
    fun `test getValue`() {
        val c: Cache = mock()
        c.retrieveValue<String>("key")
        verify(c).retrieve("key", String::class.java)

        clearInvocations(c)
        c.retrieveValue<Cache>("key")
        verify(c).retrieve("key", Cache::class.java)
    }

    @Test
    fun `test weak reference cache`() {
        val cache = WeakReferenceCacheInterpreter.eval(GetCacheExpression(""), mock())?.invoke()
                ?: throw Exception("no instance returned")
        cache.save("key", "value")
        assertEquals("value", cache.retrieve("key", String::class.java))
        assertNull(cache.retrieve("key", Boolean::class.java))
    }

    @Test
    fun `interpreter not cache`() {
        assertNull(WeakReferenceCacheInterpreter.eval(mock<Expression<String>>(), mock()))
    }

    @Test
    fun `test priority`() {
        assertTrue(WeakReferenceCacheInterpreter.priority > 5)
    }
}