package zio.redis

import zio.ZIO
import zio.redis.RedisError.ProtocolError
import zio.test.Assertion.{anything, equalTo, fails, isNonEmpty, isNonEmptyString, isSubtype, isUnit}
import zio.test.TestAspect.ignore
import zio.test.{Spec, assert, _}

trait ServerSpec extends BaseSpec {

  def serverSuite: BeginSearchSpec[Redis, RedisError] =
    suite("server")(
      test("call AclCat") {
        for {
          redis      <- ZIO.service[Redis]
          categories <- redis.aclCat
        } yield assert(categories)(isNonEmpty)
      },
      test("call AclCat with category parameter") {
        for {
          redis      <- ZIO.service[Redis]
          categories <- redis.aclCat("read")
        } yield assert(categories)(isNonEmpty)
      },
      test("call AclDeluser") {
        for {
          redis <- ZIO.service[Redis]
          _     <- redis.aclSetuser("FooBar")
          n     <- redis.aclDeluser("FooBar")
        } yield assert(n)(equalTo(1L))
      },
      test("call AclSetuser") {
        for {
          redis  <- ZIO.service[Redis]
          result <- redis.aclSetuser("FooBar")
          _      <- redis.aclDeluser("FooBar")
        } yield assert(result)(isUnit)
      },
      test("call AclSetuser with rules") {
        for {
          redis  <- ZIO.service[Redis]
          result <- redis.aclSetuser("FooBar", "on", "allkeys", "+set")
          _      <- redis.aclDeluser("FooBar")
        } yield assert(result)(isUnit)
      },
      test("call aclDryrun") {
        for {
          redis         <- ZIO.service[Redis]
          _             <- redis.aclSetuser("VIRGINIA", "+SET", "~*")
          successResult <- redis.aclDryrun("VIRGINIA", "SET", "foo", "bar")
          failureResult <- redis.aclDryrun("VIRGINIA", "GET", "foo")
          _             <- redis.aclDeluser("VIRGINIA")
        } yield assert(successResult)(equalTo("OK")) && assert(failureResult)(isSubtype[String](anything))
      },
      test("call aclGenpass") {
        for {
          redis    <- ZIO.service[Redis]
          password <- redis.aclGenpass
        } yield assert(password)(isNonEmptyString)
      },
      test("call aclGenpass with bits") {
        for {
          redis    <- ZIO.service[Redis]
          password <- redis.aclGenpass(12)
        } yield assert(password)(isNonEmptyString)
      },
      test("call aclGetuser") {
        for {
          redis <- ZIO.service[Redis]
          _     <- redis.aclSetuser("sample", "on", "nopass", "+GET", "allkeys", "&*", "(+SET ~key2)")
          user  <- redis.aclGetuser("sample")
          _     <- redis.aclDeluser("sample")
        } yield assert(user)(isNonEmpty)
      },
      test("call aclList") {
        for {
          redis <- ZIO.service[Redis]
          _     <- redis.aclSetuser("sample", "on", "nopass", "+GET", "allkeys", "&*", "(+SET ~key2)")
          acls  <- redis.aclList
          _     <- redis.aclDeluser("sample")
        } yield assert(acls)(isNonEmpty)
      },
      test("call aclLoad") {
        for {
          redis <- ZIO.service[Redis]
          result <- redis.aclLoad.exit
        } yield assert(result)(fails(isSubtype[ProtocolError](anything)))
      },
      test("call aclLog") {
        for {
          redis <- ZIO.service[Redis]
          _ <- redis.auth("someuser", "wrongpassword").either
          result <- redis.aclLog.tapError(e => ZIO.succeed(println(e)))
        } yield assert(result)(isNonEmpty)
      },
      test("call aclLog with count param") {
        for {
          redis <- ZIO.service[Redis]
          _ <- redis.auth("someuser", "wrongpassword").either
          result <- redis.aclLog(3L)
        } yield assert(result)(isNonEmpty)
      },
      test("call aclLog reset") {
        for {
          redis <- ZIO.service[Redis]
          result <- redis.aclLogReset
        } yield assert(result)(isUnit)
      },
      test("call aclSave") {
        for {
          redis <- ZIO.service[Redis]
          result <- redis.aclSave.exit
        } yield assert(result)(fails(isSubtype[RedisError](anything)))
      },
      test("call aclUsers") {
        for {
          redis <- ZIO.service[Redis]
          result <- redis.aclUsers
        } yield assert(result)(isNonEmpty)
      },
      test("call aclWhoAmI") {
        for {
          redis <- ZIO.service[Redis]
          result <- redis.aclWhoAmI
        } yield assert(result)(isNonEmptyString)
      },
      test("call bgRewriteAof") {
        for {
          redis <- ZIO.service[Redis]
          result <- redis.bgRewriteAof
        } yield assert(result)(isNonEmptyString)
      },
      test("call bgSave") {
        for {
          redis <- ZIO.service[Redis]
          result <- redis.bgSave
        } yield assert(result)(isNonEmptyString)
      } @@ ignore,
      test("call bgSaveSchedule") {
        for {
          redis <- ZIO.service[Redis]
          result <- redis.bgSaveSchedule
        } yield assert(result)(isNonEmptyString)
      }
    )
}
