package bob.free

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

class WeakReferenceCache : Cache {
    private val store = mutableMapOf<String, Any>()
    override fun <V : Any> save(key: String, v: V) {
        store[key] = v
    }

    @Suppress("UNCHECKED_CAST")
    override fun <V : Any> retrieve(key: String, type: Class<V>) =
            store[key]?.takeIf { type.isInstance(it) } as? V

}

object WeakReferenceCacheInterpreter : PartlyInterpreter {
    override val priority: Int
        get() = 10

    override fun <O : Any> eval(exp: Expression<O>, interpreter: Interpreter) =
            (exp as? GetCacheExpression)?.let {
                {
                    @Suppress("UNCHECKED_CAST")
                    WeakReferenceCache() as O
                }
            }
}