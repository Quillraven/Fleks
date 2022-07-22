# Fleks

[![MIT license](https://img.shields.io/badge/License-MIT-blue.svg)](https://github.com/Quillraven/Fleks/blob/master/LICENSE)
[![Maven](https://img.shields.io/badge/Maven-1.4.1--JVM-success.svg)](https://search.maven.org/artifact/io.github.quillraven.fleks/Fleks/1.4.1-JVM/jar)

[![Build Master](https://img.shields.io/github/workflow/status/quillraven/fleks/Build/master?event=push&label=Build%20master)](https://github.com/Quillraven/fleks/actions)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.6.21-red.svg)](http://kotlinlang.org/)

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

If you are looking for a long time verified ECS that supports Java 
then use [Artemis-odb](https://github.com/junkdog/artemis-odb) or [Ashley](https://github.com/libgdx/ashley).

## Current Status

Thanks to [jobe-m](https://github.com/jobe-m) Fleks also has a Kotlin Multiplatform version which will be the future for Fleks.
However, since KMP is still in alpha and in my opinion the developer experience is not yet there where it should be,
Fleks will come in two flavors and will also have two releases in parallel:
- **JVM** which can be used for any backend that supports a JVM like native Java applications or Android
- **KMP** which can be used for any platform and can also be used in a [KorGE](https://korge.org/) game

You can find the KMP version [here](https://github.com/Quillraven/Fleks/tree/kmp).
It has a slightly different API for the world's configuration due to limitations in reflection but after that
everything is the same as in the JVM version. And as mentioned above, in the future those
two flavors will be combined into a single one which is most likely the KMP version once I figured out
how to support a similar nice user experience as in the JVM flavor ;)

To use Fleks add it as a dependency to your project:

#### Apache Maven

```kotlin
<dependency>
  <groupId>io.github.quillraven.fleks</groupId>
  <artifactId>Fleks</artifactId>
  <version>1.4.1-JVM</version>
</dependency>
```

#### Gradle (Groovy)

```kotlin
implementation 'io.github.quillraven.fleks:Fleks:1.4.1-JVM'
```

#### Gradle (Kotlin)

```kotlin
implementation("io.github.quillraven.fleks:Fleks:1.4.1-JVM")
```

## API and examples

The API is documented in the [wiki](https://github.com/Quillraven/Fleks/wiki) that
also contains an example section for JVM and KMP projects.

## Performance

One important topic for me throughout the development of Fleks was performance. For that I compared Fleks with
Artemis-odb and Ashley in three scenarios which you can find in the **benchmarks** source set:

1) **AddRemove**: Creates 10_000 entities with a single component each and removes those entities.
2) **Simple**: Steps the world 1_000 times for 10_000 entities with an `IteratingSystem` for a single component that
   gets a `Float` counter increased by one every tick.
3) **Complex**: Steps the world 1_000 times for 10_000 entities with two `IteratingSystem` and three components. It is a
   time-consuming benchmark because all entities get added and removed from the first system each tick.
    - Each entity gets initialized with ComponentA and ComponentC.
    - The first system requires ComponentA, ComponentC and not ComponentB. It switches between creating ComponentB or
      removing ComponentA. That way every entity gets removed from this system each tick.
    - The second system requires any ComponentA/B/C and removes ComponentB and adds ComponentA. That way every entity
      gets added again to the first system.

I used [kotlinx-benchmark](https://github.com/Kotlin/kotlinx-benchmark) to create the benchmarks with a measurement
that represents the number of executed operations within three seconds.

All Benchmarks are run within IntelliJ using the `benchmarksBenchmark` gradle task on my local computer. The hardware
is:

- Windows 10 64-bit
- 16 GB Ram
- Intel i7-5820K @ 3.30Ghz
- Java 8 target

Here is the result (the higher the Score the better):

| Library | Benchmark | Mode | Cnt | Score | Error | Units |
| ------- | --------- | ---- | --- | ----- | ----- | ----- |
| |
| Ashley | AddRemove | thrpt | 3 | 207,007 | ± 39,121 | ops/s |
| Artemis | AddRemove | thrpt | 3 | 677,231 | ± 2002,449 | ops/s |
| Fleks | AddRemove | thrpt | 3 | 806,189 | ± 249,523 | ops/s |
| |
| Ashley | Simple | thrpt | 3 | 3,986 | ± 1,390 | ops/s |
| Artemis | Simple | thrpt | 3 | 32,830 | ± 2,965 | ops/s |
| Fleks | Simple | thrpt | 3 | 32,639 | ± 5,651 | ops/s |
| |
| Ashley | Complex | thrpt | 3 | 0,056 | ± 0,117 | ops/s |
| Artemis | Complex | thrpt | 3 | 1,452 | ± 0,452 | ops/s |
| Fleks | Complex | thrpt | 3 | 1,196 | ± 0,210 | ops/s |

I am not an expert for performance measurement, that's why you should take those numbers with a grain of salt but as you
can see in the table:

- Ashley is the slowest of the three libraries by far
- Fleks is ~1.2x the speed of Artemis in the **AddRemove** benchmark
- Fleks is ~the same speed as Artemis in the **Simple** benchmark
- Fleks is ~0.8x the speed of Artemis in the **Complex** benchmark
