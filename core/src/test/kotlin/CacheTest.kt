import bob.free.*
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Test

class CacheTest {
    @Test
    fun `test getValue`() {
        val c: Cache = mockk(relaxed = true)
        c.retrieveValue<String>("key")
        verify { c.retrieve("key", String::class.java) }

        clearMocks(c)
        c.retrieveValue<Cache>("key")
        verify { c.retrieve("key", Cache::class.java) }
    }

    @Test
    fun `test weak reference cache`() {
        `test reference cache`(ReferenceType.WEAK)
    }

    @Test
    fun `test soft reference cache`() {
        `test reference cache`(ReferenceType.SOFT)
    }

    private fun `test reference cache`(type: ReferenceType) {
        val cache = ReferenceCacheInterpreter(type).eval(GetCacheExpression(""), mockk())?.invoke()
                ?: throw Exception("no instance returned")
        cache.save("key", "value")
        assertEquals("value", cache.retrieve("key", String::class.java))
        assertNull(cache.retrieve("key", Boolean::class.java))
    }

    @Test
    fun `interpreter not cache`() {
        assertNull(
            ReferenceCacheInterpreter(ReferenceType.WEAK).eval(
                mockk<Expression<String>>(),
                mockk()
            )
        )
    }

    @Test
    fun `test priority`() {
        assertTrue(ReferenceCacheInterpreter(ReferenceType.WEAK).priority > 5)
        assertTrue(ReferenceCacheInterpreter(ReferenceType.SOFT).priority > 5)
    }
}