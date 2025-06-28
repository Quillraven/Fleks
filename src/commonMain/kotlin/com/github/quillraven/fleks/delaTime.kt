

interface WorldClock<T> {
    val deltaTime: T
}


class NoOpWorldClock : WorldClock<Unit> {
    override val deltaTime = Unit
}
