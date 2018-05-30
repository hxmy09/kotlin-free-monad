package bob.free

import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

interface Cache {
    fun <V : Any> save(key: String, v: V)
    fun <V : Any> retrieve(key: String, type: Class<V>): V?
}

class HierarchyCache(private val layers: List<Cache>) : Cache {
    override fun <V : Any> save(key: String, v: V) {
        layers.forEach { it.save(key, v) }
    }

    override fun <V : Any> retrieve(key: String, type: Class<V>): V? =
        layers.fold(null as V?) { v, cache -> v ?: cache.retrieve(key, type) }

}

inline fun <reified V : Any> Cache.retrieveValue(key: String) = retrieve(key, V::class.java)

data class GetCacheExpression(val name: String) : Expression<Cache>

enum class ReferenceType {
    WEAK, SOFT
}

class ReferenceCache(type: ReferenceType) : Cache {
    private val store = mutableMapOf<String, Reference<Any>>()
    private val createRef = when (type) {
        ReferenceType.WEAK -> { v: Any -> WeakReference(v) }
        ReferenceType.SOFT -> { v: Any -> SoftReference(v) }
    }

    override fun <V : Any> save(key: String, v: V) {
        store[key] = createRef(v)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <V : Any> retrieve(key: String, type: Class<V>) =
        store[key]?.get()?.takeIf { type.isInstance(it) } as? V
}

class ReferenceCacheInterpreter(private val type: ReferenceType) : PartlyInterpreter {
    private val caches = mutableMapOf<String, ReferenceCache>()

    override val priority: Int
        get() = 10

    override fun <O : Any> eval(exp: Expression<O>, interpreter: Interpreter) =
        (exp as? GetCacheExpression)?.let {
            {
                @Suppress("UNCHECKED_CAST")
                caches.getOrPut(it.name) { ReferenceCache(type) } as O
            }
        }
}