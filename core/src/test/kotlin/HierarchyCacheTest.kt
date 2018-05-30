import bob.free.Cache
import bob.free.HierarchyCache
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class HierarchyCacheTest {
    @Test
    fun save() {
        val layers = listOf<Cache>(mockk(relaxed = true), mockk(relaxed = true))
        HierarchyCache(layers).save("key", "value")
        layers.forEach { verify { it.save("key", "value") } }
    }

    @Test
    fun retrieve() {
        val layers = listOf(mockk<Cache>().also {
            every { it.retrieve("key", String::class.java) } returns null
        }, mockk<Cache>().also {
            every { it.retrieve("key", String::class.java) } returns "value"
        })
        assertEquals("value", HierarchyCache(layers).retrieve("key", String::class.java))
        layers.forEach { verify { it.retrieve("key", String::class.java) } }
    }

    @Test
    fun retrieveOnFirst() {
        val layers = listOf(mockk<Cache>().also {
            every { it.retrieve("key", String::class.java) } returns "value"
        }, mockk<Cache>().also {
            every { it.retrieve("key", String::class.java) } returns "value"
        })
        assertEquals("value", HierarchyCache(layers).retrieve("key", String::class.java))
        layers[0].let { verify { it.retrieve("key", String::class.java) } }
        layers.drop(1).forEach { verify { it wasNot called } }
    }

}