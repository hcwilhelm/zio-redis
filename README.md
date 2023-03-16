[//]: # (This file was autogenerated using `zio-sbt-website` plugin via `sbt generateReadme` command.)
[//]: # (So please do not edit it manually. Instead, change "docs/index.md" file or sbt setting keys)
[//]: # (e.g. "readmeDocumentation" and "readmeSupport".)

# ZIO Redis

[ZIO Redis](https://github.com/zio/zio-redis) is a ZIO native Redis client.

[![Experimental](https://img.shields.io/badge/Project%20Stage-Experimental-yellowgreen.svg)](https://github.com/zio/zio/wiki/Project-Stages) ![CI Badge](https://github.com/zio/zio-redis/workflows/CI/badge.svg) [![Sonatype Releases](https://img.shields.io/nexus/r/https/oss.sonatype.org/dev.zio/zio-redis_2.13.svg?label=Sonatype%20Release)](https://oss.sonatype.org/content/repositories/releases/dev/zio/zio-redis_2.13/) [![Sonatype Snapshots](https://img.shields.io/nexus/s/https/oss.sonatype.org/dev.zio/zio-redis_2.13.svg?label=Sonatype%20Snapshot)](https://oss.sonatype.org/content/repositories/snapshots/dev/zio/zio-redis_2.13/) [![javadoc](https://javadoc.io/badge2/dev.zio/zio-redis-docs_2.13/javadoc.svg)](https://javadoc.io/doc/dev.zio/zio-redis-docs_2.13) [![ZIO Redis](https://img.shields.io/github/stars/zio/zio-redis?style=social)](https://github.com/zio/zio-redis)

## Introduction

ZIO Redis is in the experimental phase of development, but its goals are:

- **Type Safety**
- **Performance**
- **Minimum Dependency**
- **ZIO Native**

## Installation

Since the ZIO Redis is in the experimental phase, it is not released yet, but we can use snapshots:

```scala
libraryDependencies += "dev.zio" %% "zio-redis" % "<version>"
```

## Example

To execute our ZIO Redis effect, we should provide the `RedisExecutor` layer to that effect. To create this layer we
should also provide the following layers:

- **RedisConfig** — Using default one, will connect to the `localhost:6379` Redis instance.
- **BinaryCodec** — In this example, we are going to use the built-in `ProtobufCodec` codec from zio-schema project.

To run this example we should put following dependencies in our `build.sbt` file:

```scala
libraryDependencies ++= Seq(
  "dev.zio" %% "zio-redis" % "<version>",
  "dev.zio" %% "zio-schema-protobuf" % "0.3.0"
)
```

```scala
import zio._
import zio.redis._
import zio.schema.codec._

object ZIORedisExample extends ZIOAppDefault {
  val myApp: ZIO[Redis, RedisError, Unit] = for {
    redis <- ZIO.service[Redis]
    _     <- redis.set("myKey", 8L, Some(1.minutes))
    v     <- redis.get("myKey").returning[Long]
    _     <- Console.printLine(s"Value of myKey: $v").orDie
    _     <- redis.hSet("myHash", ("k1", 6), ("k2", 2))
    _     <- redis.rPush("myList", 1, 2, 3, 4)
    _     <- redis.sAdd("mySet", "a", "b", "a", "c")
  } yield ()

  override def run = myApp.provide(
    Redis.layer,
    RedisExecutor.layer,
    ZLayer.succeed(RedisConfig.Default),
    ZLayer.succeed[BinaryCodec](ProtobufCodec)
  )
}
```

## Testing

To test you can use the embedded redis instance by adding to your build:

```libraryDependencies := "dev.zio" %% "zio-redis-embedded" % <version>```

Then you can supply `EmbeddedRedis.layer.orDie` as your `RedisConfig` and you're good to go!

```scala
import zio._
import zio.redis._
import zio.schema.{DeriveSchema, Schema}
import zio.schema.codec.{BinaryCodec, ProtobufCodec}
import zio.test._
import zio.test.Assertion._
import java.util.UUID
object EmbeddedRedisSpec extends ZIOSpecDefault {
  final case class Item private (id: UUID, name: String, quantity: Int)
  object Item {
    implicit val itemSchema: Schema[Item] = DeriveSchema.gen[Item]
  }
  def spec = suite("EmbeddedRedis should")(
    test("set and get values") {
      for {
        redis <- ZIO.service[Redis]
        item   = Item(UUID.randomUUID, "foo", 2)
        _     <- redis.set(s"item.${item.id.toString}", item)
        found <- redis.get(s"item.${item.id.toString}").returning[Item]
      } yield assert(found)(isSome(equalTo(item)))
    }
  ).provideShared(
    EmbeddedRedis.layer.orDie,
    RedisExecutor.layer.orDie,
    ZLayer.succeed[BinaryCodec](ProtobufCodec),
    Redis.layer
  ) @@ TestAspect.silentLogging
}
```

## Resources

- [ZIO Redis](https://www.youtube.com/watch?v=yqFt3b3RBkI) by Dejan Mijic — Redis is one of the most commonly used
  in-memory data structure stores. In this talk, Dejan will introduce ZIO Redis, a purely functional, strongly typed
  client library backed by ZIO, with excellent performance and extensive support for nearly all of Redis' features. He
  will explain the library design using the bottom-up approach - from communication protocol to public APIs. Finally, he
  will wrap the talk by demonstrating the client's usage and discussing its performance characteristics.

## Documentation

Learn more on the [ZIO Redis homepage](https://zio.dev/zio-redis/)!

## Contributing

For the general guidelines, see ZIO [contributor's guide](https://zio.dev/about/contributing).

## Code of Conduct

See the [Code of Conduct](https://zio.dev/about/code-of-conduct)

## Support

Come chat with us on [![Badge-Discord]][Link-Discord].

[Badge-Discord]: https://img.shields.io/discord/629491597070827530?logo=discord "chat on discord"
[Link-Discord]: https://discord.gg/2ccFBr4 "Discord"

## License

[License](LICENSE)
