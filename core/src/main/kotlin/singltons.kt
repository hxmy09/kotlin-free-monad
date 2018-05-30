package bob.free

object Singletons {
    @JvmStatic
    fun <T> addInstance(type: Class<T>, init: () -> T) = also {
        instances[type] = lazy(init)
    }

    @JvmStatic
    fun <T> getInstance(type: Class<T>) =
            Result {
                @Suppress("UNCHECKED_CAST")
                (instances[type] as? Lazy<T>)?.value
                        ?: throw RuntimeException("singleton has not been set yet")
            }

    private val instances = mutableMapOf<Class<*>, Lazy<*>>()
}

// for kotlin classes that support reified generic
inline fun <reified T> add(noinline init: () -> T) =
        Singletons.addInstance(T::class.java, init)

inline fun <reified T> get() = Singletons.getInstance(T::class.java)
