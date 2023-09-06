package zio.redis

import zio._
import zio.test.TestAspect._
import zio.test._

object ApiSpec
    extends ConnectionSpec
    with KeysSpec
    with ListSpec
    with SetsSpec
    with SortedSetsSpec
    with StringsSpec
    with GeoSpec
    with HyperLogLogSpec
    with HashSpec
    with StreamsSpec
    with ScriptingSpec
    with ClusterSpec
    with PubSubSpec
    with ServerSpec {

  def spec: BeginSearchSpec[TestEnvironment, Any] =
    suite("Redis commands")(singleNodeSuite) @@ sequential @@ withLiveEnvironment

  private val singleNodeSuite =
    suite("Single node executor")(
      serverSuite
    ).provideShared(Redis.local, RedisSubscription.local, ZLayer.succeed(ProtobufCodecSupplier))
}
