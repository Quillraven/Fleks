# Fleks

A **f**ast, **l**ightweight, **e**ntity component **s**ystem library written in **K**otlin.

## Motivation

When developing my hobby games using [LibGDX](https://github.com/libgdx/libgdx), I always
used [Ashley](https://github.com/libgdx/ashley)
as an [**E**ntity **C**omponent **S**ystem](https://en.wikipedia.org/wiki/Entity_component_system) since it comes out of
the box with LibGDX and performance wise it was always good enough for me.

When using [Kotlin](https://kotlinlang.org/) and [LibKTX](https://github.com/libktx/ktx) you even get nice extension
functions for it but I never was fully happy with how it felt because:

- Defining [ComponentMapper](https://github.com/libgdx/ashley/wiki/How-to-use-Ashley#retrieving-components-with-componentmapper)
for every [Component](https://github.com/libgdx/ashley/wiki/How-to-use-Ashley#components) felt very redundant
- Ashley is not null-safe and therefore you get e.g. `Entity?` passed in as default to
  an [IteratingSystem](https://github.com/libgdx/ashley/wiki/Built-in-Entity-Systems#iteratingsystem)
  although it will never be null (or at least shouldn't 😉)
- Creating [Families](https://github.com/libgdx/ashley/wiki/How-to-use-Ashley#entity-families) as constructor arguments
  felt weird to me
- The creator seems to not work on this project anymore or at least reacts super slowly to change requests, bugs or
  improvement ideas
- Although for me it was never a problem, I heard from other people that the performance is sometimes bad especially
  with a lot of [entities](https://github.com/libgdx/ashley/wiki/How-to-use-Ashley#entities) that get their components
  updated each frame

Those are the reasons why I wanted to build my own ECS-library and of course out of interest to dig deep into the
details of this topic and to learn something new!

## Who should use this library?

If you need a lightweight and fast ECS in your Kotlin application then feel free to use Fleks.

If you are looking for a fully fledged ECS that supports almost anything that you can imagine and you
don't care about Kotlin then use [Artemis-odb](https://github.com/junkdog/artemis-odb) or [Ashley](https://github.com/libgdx/ashley).

## Current Status

There is no official release yet and the library is still under construction. Please feel free to contribute to the
Discussions or Issues. Help is always appreciated.

## Current API and usage (not final)

### World

The core of Fleks is the `World` which is the container for entities, components and systems and is the object that
you need to update your systems.

To create a world simply call:

```Kotlin
val world = World {}
```

A world without any **system** doesn't make sense and that's why there is a lambda argument for the world's constructor
to configure it accordingly:

- Use `entityCapacity` to set the expected maximum amount of entities. The default value is 512. The reason for this
  setting is to initialize internal collections and arrays with a proper size for your game to avoid a lot
  of `Array.copy` calls in the background which are slow.
- Use `system` to add a system to your world. The order of `system` calls defines the order in which they are called
  when calling `world.update`

Here is an example that creates a world for 1000 entities with a Move- and PhysicSystem:

```Kotlin
val world = World {
    entityCapacity = 1000

    system<MoveSystem>()
    system<PhysicSystem>()
}
```

### System

Usually, your systems depend on certain other things like
a [SpriteBatch](https://github.com/libgdx/libgdx/wiki/Spritebatch%2C-Textureregions%2C-and-Sprites#SpriteBatch)
or [Viewport](https://github.com/libgdx/libgdx/wiki/Spritebatch%2C-Textureregions%2C-and-Sprites#Viewport). Fleks
uses [dependency injection](https://de.wikipedia.org/wiki/Dependency_Injection) for that to make it easier to adjust
arguments of your systems later on without touching the code of the caller side.

First, let's have a look on how to create a simple **IteratingSystem** that gets called every time `world.update` is
called. It is a made up example of a Day-Night-Cycle system which switches between day and night every second and
dispatches a game event via an `EventManager`.

```Kotlin
class DayNightSystem(
    private val eventMgr: EventManager
) : EntitySystem() {
    private var currentTime = 0f
    private var isDay = false

    override fun onTick(deltaTime: Float) {
        currentTime += deltaTime
        if (currentTime >= 1000 && !isDay) {
            isDay = true
            eventMgr.publishDayEvent()
        } else if (currentTime >= 2000 && isDay) {
            isDay = false
            currentTime = 0f
            eventMgr.publishNightEvent()
        }
    }
}
```

The `DayNightSystem` requires an `EventManager` which we need to inject. To achieve that we can define it when creating
our world by using the `inject` function:

```Kotlin
val eventManager = EventManager()
val world = World {
    entityCapacity = 1000

    system<DayNightSystem>()

    inject(eventManager)
}
```

There are two systems in Fleks:

- `EntitySystem`: system without relation to entities.
- `IteratingSystem`: system with relation to entities of a specific component configuration.

`EntitySystem` has two optional arguments:

- `tickRate`: defines the time in milliseconds when the system should be updated. Default is 0 means that it gets called
  every time `world.update` is called
- `enabled`: defines if the system will be processed or not. Default value is true.

`IteratingSystem` extends `EntitySystem` but in addition it requires you to specify the relevant components of entities
which the system will iterate over. There are three class annotations to define this component configuration:

- `AllOf`: entity must have all the components specified
- `NoneOf`: entity must not have any component specified
- `AnyOf`: entity must have at least one of the components specified

Let's create an `IteratingSystem` that iterates over all entities with a `PositionComponent`, `PhysicComponent`
and at least a `SpriteComponent` or `AnimationComponent` but without a `DeadComponent`:

```Kotlin
@AllOf([Position::class, Physic::class])
@NoneOf([Dead::class])
@AnyOf([Sprite::class, Animation::class])
class AnimationSystem() : IteratingSystem() {
    override fun onEntityAction(entityId: Int, deltaTime: Float) {
        // update entities in here
    }
}
```

Often, an `IteratingSystem` needs access to the components of an entity. In Fleks this is done via so
called `ComponentMapper`. `ComponentMapper` are automatically injected into a system and do not need to be defined in
the world's configuration.

Let's see how we can access the `PositionComponent` of an entity in the system above:

```Kotlin
@AllOf([Position::class, Physic::class])
@NoneOf([Dead::class])
@AnyOf([Sprite::class, Animation::class])
class AnimationSystem(
    private val positions: ComponentMapper<Position>
) : IteratingSystem() {
    override fun onEntityAction(entityId: Int, deltaTime: Float) {
        val entityPosition: Position = positions[entityId]
    }
}
```

### Entity and Components

We now know how to create a world and add systems to it, but we don't know how to add entities to our world. This can be
done via the `entity` function of the world. Let's create an entity with a `PositionComponent` and `SpriteComponent`:

```Kotlin
data class Position(var x: Float = 0f, var y: Float = 0f)

data class Sprite(var texturePath: String = "")

fun main() {
    val world = World {}

    val entityId: Int = world.entity {
        add<Position> { x = 5f }
        add<Sprite>()
    }
}
```

## Performance

One important topic for me throughout the development of Fleks was performance.
For that I compared Fleks with Artemis-odb and Ashley in three scenarios:
1) **AddRemove**: creating 10_000 entities with a single component each and removing those entities
2) **Simple**: stepping the world 1_000 times for 10_000 entities with an IteratingSystem for a single component
that gets a `Float` counter increased by one every time
3) **Complex**: stepping the world 1_000 times for 10_000 entities, two IteratingSystems and three components.
It is a time-consuming benchmark because all entities get added/removed from the first system each update call.
   - Each entity gets initialized with ComponentA and ComponentC.
   - The first system requires ComponentA, ComponentC and not ComponentB. It switches between creating ComponentB or removing ComponentA.
   That way every entity gets removed from this system each update call.
   - The second system requires any ComponentA/B/C and removes ComponentC and adds ComponentA.
   That way every entity gets added again for the first system.
 
I used [kotlinx-benchmark](https://github.com/Kotlin/kotlinx-benchmark) to create the benchmarks.
Measurement is number of executed operations within 3 seconds.

All Benchmarks are run within IntelliJ using the `benchmarksBenchmark` gradle task on my local computer.
The hardware is:
- Windows 10 64-bit
- 16 GB Ram
- Intel i7-5820K @ 3.30Ghz
- Java 8 target
- Kotlin 1.5.31

Here is the result (the higher the Score the better):

| Library | Benchmark | Mode | Cnt | Score | Error | Units |
| ------- | --------- | ---- | --- | ----- | ----- | ----- |
| |
| Ashley | AddRemove | thrpt | 3 | 207,007 | ± 39,121 | ops/s |
| Ashley | Simple | thrpt | 3 | 3,986 | ± 1,390 | ops/s |
| Ashley | Complex | thrpt | 3 | 0,056 | ± 0,117 | ops/s |
| |
| Artemis | AddRemove | thrpt | 3 | 620,550 | ± 1244,013 | ops/s |
| Artemis | Simple | thrpt | 3 | 32,830 | ± 2,965 | ops/s |
| Artemis | Complex | thrpt | 3 | 1,452 | ± 0,452 | ops/s |
| |
| Fleks | AddRemove | thrpt | 3 | 1904,151 | ± 530,426 | ops/s |
| Fleks | Simple | thrpt | 3 | 33,639 | ± 5,651 | ops/s |
| Fleks | Complex | thrpt | 3 | 1,063 | ± 0,374 | ops/s |

I am not an expert for performance measurement, that's why you should take those numbers with a grain of salt but
as you can see in the table:
- Ashley is the slowest of the three libraries by far
- Fleks is ~300% the speed of Artemis in the **AddRemove** benchmark
- Fleks is ~the same speed as Artemis in the **Simple** benchmark
- Fleks is ~70% the speed of Artemis in the **Complex** benchmark

As an additional note please be aware that Fleks does not support all the functionalities that the other two libraries offer.
Fleks' core is very small and simple and therefore it does not need to process as much things as Ashley or Artemis might.
Still, in those benchmarks all libraries have to fulfill the same need which reflects some common tasks in my own games.
