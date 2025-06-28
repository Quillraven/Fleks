

interface WorldClock<T> {
    val deltaTime: T

    fun update(deltaTime: T)
}


class NoOpWorldClock : WorldClock<Unit> {
    override val deltaTime = Unit
    override fun update(deltaTime: Unit)  = Unit
}
