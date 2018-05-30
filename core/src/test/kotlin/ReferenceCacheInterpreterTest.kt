import bob.free.GetCacheExpression
import bob.free.ReferenceCacheInterpreter
import bob.free.ReferenceType
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
class ReferenceCacheInterpreterTest {

    @Test
    fun testEval() {
        val i = ReferenceCacheInterpreter(ReferenceType.WEAK)
        assertNotNull(i.eval(GetCacheExpression("n"), mockk(relaxed = true))?.invoke())
        // same key, same cache
        assertEquals(
            i.eval(GetCacheExpression("n1"), mockk(relaxed = true))?.invoke(),
            i.eval(GetCacheExpression("n1"), mockk(relaxed = true))?.invoke()
        )
    }
}