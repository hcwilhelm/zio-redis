package zio.redis

import zio._
import zio.schema.Schema
import zio.schema.codec.{BinaryCodec, ProtobufCodec}
import zio.test.TestAspect.{fibers, silentLogging, timeout}
import zio.test._

trait BaseSpec extends ZIOSpecDefault {
  implicit def summonCodec[A: Schema]: BinaryCodec[A] = ProtobufCodec.protobufCodec

  override def aspects: Chunk[TestAspectAtLeastR[Live]] =
    Chunk(fibers, silentLogging, timeout(10.seconds))
}
