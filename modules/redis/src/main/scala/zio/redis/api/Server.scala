/*
 * Copyright 2021 John A. De Goes and the ZIO contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zio.redis.api

import zio.redis.Input.{LongInput, NoInput, NonEmptyList, StringInput}
import zio.redis.Output._
import zio.redis.RedisError
import zio.redis.internal.{RedisCommand, RedisEnvironment}
import zio.{Chunk, IO}
import zio.redis.{ArrayEntry, LogEntry, CommandEntry}

trait Server extends RedisEnvironment {

  import Server._

  /**
   * The command shows the available ACL categories if called without arguments.
   *
   * @return
   * returns a list of category names.
   */
  final def aclCat: IO[RedisError, Chunk[String]] = {
    val command = RedisCommand(AclCat, NoInput, ChunkOutput(MultiStringOutput), executor)
    command.run(())
  }

  /**
   * If a category name is given, the command shows all the Redis commands in the specified category.
   *
   * @param cat
   * category name
   * @return
   * returns a list of command names.
   */
  final def aclCat(category: String): IO[RedisError, Chunk[String]] = {
    val command = RedisCommand(AclCat, StringInput, ChunkOutput(MultiStringOutput), executor)
    command.run(category)
  }

  /**
   * Delete all the specified ACL users and terminate all the connections that are authenticated with such users.
   * Note: the special default user cannot be removed from the system, this is the default user that every new
   * connection is authenticated with. The list of users may include usernames that do not exist, in such case no
   * operation is performed for the non existing users.
   *
   * @param user
   * user name
   * @return
   * number of deleted users
   */
  final def aclDeluser(user: String): IO[RedisError, Long] = {
    val command = RedisCommand(AclDeluser, StringInput, LongOutput, executor)
    command.run(user)
  }

  /**
   * Create an ACL user with the specified rules or modify the rules of an existing user.
   *
   * @param user
   * user name
   * @param rules
   * variable number of rules
   * @return
   * unit
   */
  final def aclSetuser(user: String, rules: String*): IO[RedisError, Unit] = {
    val command = RedisCommand(AclSetuser, NonEmptyList(StringInput), UnitOutput, executor)
    command.run((user, rules.toList))
  }

  /**
   * Simulate the execution of a given command by a given user. This command can be used to test the permissions
   * of a given user without having to enable the user or cause the side effects of running the command.
   *
   * @param user
   * user name
   * @param dryCommand
   * the command to simulate
   * @param args
   * paramters for the command to simulate
   * @return
   * returns OK or an error description
   */
  final def aclDryrun(user: String, dryCommand: String, args: String*): IO[RedisError, String] = {
    val command = RedisCommand(AclDryrun, NonEmptyList(StringInput), AclDryrunOutput, executor)
    command.run((user, dryCommand :: args.toList))
  }

  /**
   * ACL users need a solid password in order to authenticate to the server without security risks.
   *
   * @return
   * 256 bits of pseudorandom data.
   */
  final def aclGenpass: IO[RedisError, String] = {
    val command = RedisCommand(AclGenpass, NoInput, MultiStringOutput, executor)
    command.run(())
  }

  /**
   * ACL users need a solid password in order to authenticate to the server without security risks.
   *
   * @param bits
   * password length in bits
   * @return
   * bits of pseudorandom data.
   */
  final def aclGenpass(bits: Int): IO[RedisError, String] = {
    val command = RedisCommand(AclGenpass, StringInput, MultiStringOutput, executor)
    command.run(bits.toString)
  }

  /**
   * The command returns all the rules defined for an existing ACL user.
   *
   * @param user
   * user name
   * @return
   * a nested list of rules
   */
  final def aclGetuser(user: String): IO[RedisError, Chunk[ArrayEntry[String]]] = {
    val command = RedisCommand(AclGetuser, StringInput, ChunkOutput(ArrayEntryOutput(MultiStringOutput)), executor)
    command.run(user)
  }

  /**
   * The command shows the currently active ACL rules in the Redis server. Each line in the returned array defines
   * a different user, and the format is the same used in the redis.conf file or the external ACL file
   *
   * @return
   *   list of ACL rules
   */
  final def aclList: IO[RedisError, Chunk[String]] = {
    val command = RedisCommand(AclList, NoInput, ChunkOutput(MultiStringOutput), executor)
    command.run(())
  }

  /**
   * When Redis is configured to use an ACL file (with the aclfile configuration option), this command will reload
   * the ACLs from the file, replacing all the current ACL rules with the ones defined in the file.
   *
   * @return
   *  unit
   */
  final def aclLoad: IO[RedisError, Unit] = {
    val command = RedisCommand(AclLoad, NoInput, UnitOutput, executor)
    command.run(())
  }

  /**
   * The command shows a list of recent ACL security events:
   *
   * @return
   *  list of log entries
   */
  final def aclLog: IO[RedisError, Chunk[LogEntry]] = {
    val command = RedisCommand(AclLog, NoInput, ChunkOutput(LogEntryOutput), executor)
    command.run(())
  }

  /**
   * The command shows a list of recent ACL security events:
   *
   * @param count
   *  max number of log entries
   * @return
   *  list of log entries
   */
  final def aclLog(count: Long): IO[RedisError, Chunk[LogEntry]] = {
    val command = RedisCommand(AclLog, LongInput, ChunkOutput(LogEntryOutput), executor)
    command.run(count)
  }

  /**
   * Reset the list of log entries
   *
   * @return
   *  unit
   */
  final def aclLogReset: IO[RedisError, Unit] = {
    val command = RedisCommand(AclLog, StringInput, UnitOutput, executor)
    command.run("RESET")
  }

  final def aclSave: IO[RedisError, Unit] = {
    val command = RedisCommand(AclSave, NoInput, UnitOutput, executor)
    command.run(())
  }

  final def aclUsers: IO[RedisError, Chunk[String]] = {
    val command = RedisCommand(AclUsers, NoInput, ChunkOutput(MultiStringOutput), executor)
    command.run(())
  }

  final def aclWhoAmI: IO[RedisError, String] = {
    val command = RedisCommand(AclWhoAmI, NoInput, MultiStringOutput, executor)
    command.run(())
  }

  final def bgRewriteAof: IO[RedisError, String] = {
    val command = RedisCommand(BgRewriteAof, NoInput, StringOutput, executor)
    command.run(())
  }

  final def bgSave: IO[RedisError, String] = {
    val command = RedisCommand(BgSave, NoInput, StringOutput, executor)
    command.run(())
  }

  final def bgSaveSchedule: IO[RedisError, String] = {
    val command = RedisCommand(BgSave, StringInput, StringOutput, executor)
    command.run("SCHEDULE")
  }

  final def command: IO[RedisError, Chunk[CommandEntry]] = {
    val command = RedisCommand(Command, NoInput, ChunkOutput(CommandEntryOutput))
  }
}

private[redis] object Server {
  final val AclCat = "ACL CAT"
  final val AclDeluser = "ACL DELUSER"
  final val AclSetuser = "ACL SETUSER"
  final val AclDryrun = "ACL DRYRUN"
  final val AclGenpass = "ACL GENPASS"
  final val AclGetuser = "ACL GETUSER"
  final val AclList = "ACL LIST"
  final val AclLoad = "ACL LOAD"
  final val AclLog = "ACL LOG"
  final val AclSave = "ACL SAVE"
  final val AclUsers = "ACL USERS"
  final val AclWhoAmI = "ACL WHOAMI"
  final val BgRewriteAof = "BGREWRITEAOF"
  final val BgSave = "BGSAVE"
  final val Command = "COMMAND"
}
