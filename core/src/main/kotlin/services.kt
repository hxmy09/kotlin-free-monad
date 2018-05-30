package bob.free

/**
 * The expression to provide implementation for an interface, for instance retrofit::create
 */
class ServiceExpression<S : Any>(val serviceType: Class<S>) : Expression<S>
