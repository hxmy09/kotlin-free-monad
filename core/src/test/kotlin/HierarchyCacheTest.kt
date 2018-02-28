package bob.free
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Test

class HierarchyCacheTest {
    @Test
    fun save() {
        val layers = listOf<Cache>(mock(), mock())
        HierarchyCache(layers).save("key", "value")
        layers.forEach { verify(it).save("key", "value") }
    }

    @Test
    fun retrieve() {
        val layers = listOf(mock<Cache>().also {
            whenever(it.retrieve("key", String::class.java)).thenReturn(null)
        }, mock<Cache>().also {
            whenever(it.retrieve("key", String::class.java)).thenReturn("value")
        })
        assertEquals("value", HierarchyCache(layers).retrieve("key", String::class.java))
        layers.forEach { verify(it).retrieve("key", String::class.java) }
    }

    @Test
    fun retrieveOnFirst() {
        val layers = listOf(mock<Cache>().also {
            whenever(it.retrieve("key", String::class.java)).thenReturn("value")
        }, mock<Cache>().also {
            whenever(it.retrieve("key", String::class.java)).thenReturn("value")
        })
        assertEquals("value", HierarchyCache(layers).retrieve("key", String::class.java))
        layers[0].let { verify(it).retrieve("key", String::class.java) }
        layers.drop(1).forEach { verifyZeroInteractions(it) }
    }

}