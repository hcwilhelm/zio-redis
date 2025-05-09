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

import zio._
import zio.redis.Input._
import zio.redis.Output._
import zio.redis.ResultBuilder._
import zio.redis._
import zio.redis.internal.{RedisCommand, RedisEnvironment}
import zio.schema.Schema

import java.time.Instant

trait Keys[G[+_]] extends RedisEnvironment[G] {
  import Keys.{Keys => _, _}

  /**
   * Copies the value stored at the source key to the destination key.
   *
   * @param source
   *   source key
   * @param destination
   *   destination key
   * @param database
   *   optional database index
   * @param replace
   *   if this is non-empty, the destination will be removed before copying the value to it
   * @return
   *   true if source was copied, false otherwise.
   */

  final def copy[S: Schema, D: Schema](
    source: S,
    destination: D,
    database: Option[Long] = None,
    replace: Option[Replace] = None
  ): G[Boolean] = {
    val command = RedisCommand(
      Copy,
      Tuple4(ArbitraryKeyInput[S](), ArbitraryKeyInput[D](), OptionalInput(DbInput), OptionalInput(ReplaceInput)),
      BoolOutput
    )
    command.run((source, destination, database, replace))
  }

  /**
   * Removes the specified keys. A key is ignored if it does not exist.
   *
   * @param key
   *   one required key
   * @param keys
   *   maybe rest of the keys
   * @return
   *   The number of keys that were removed.
   *
   * @see
   *   [[unlink]]
   */
  final def del[K: Schema](key: K, keys: K*): G[Long] = {
    val command = RedisCommand(Del, NonEmptyList(ArbitraryKeyInput[K]()), LongOutput)
    command.run((key, keys.toList))
  }

  /**
   * Serialize the value stored at key in a Redis-specific format and return it to the user.
   *
   * @param key
   *   key
   * @return
   *   bytes for value stored at key.
   */
  final def dump[K: Schema](key: K): G[Chunk[Byte]] = {
    val command = RedisCommand(Dump, ArbitraryKeyInput[K](), BulkStringOutput)
    command.run(key)
  }

  /**
   * The number of keys existing among the ones specified as arguments. Keys mentioned multiple times and existing are
   * counted multiple times.
   *
   * @param key
   *   one required key
   * @param keys
   *   maybe rest of the keys
   * @return
   *   The number of keys existing.
   */
  final def exists[K: Schema](key: K, keys: K*): G[Long] = {
    val command = RedisCommand(Exists, NonEmptyList(ArbitraryKeyInput[K]()), LongOutput)
    command.run((key, keys.toList))
  }

  /**
   * Set a timeout on key. After the timeout has expired, the key will automatically be deleted.
   *
   * @param key
   *   key
   * @param timeout
   *   timeout
   * @return
   *   true, if the timeout was set, false if the key didn't exist.
   *
   * @see
   *   [[expireAt]]
   */
  final def expire[K: Schema](key: K, timeout: Duration): G[Boolean] = {
    val command =
      RedisCommand(Expire, Tuple2(ArbitraryKeyInput[K](), DurationSecondsInput), BoolOutput)
    command.run((key, timeout))
  }

  /**
   * Deletes the key at the specific timestamp. A timestamp in the past will delete the key immediately.
   *
   * @param key
   *   key
   * @param timestamp
   *   an absolute Unix timestamp (seconds since January 1, 1970)
   * @return
   *   true, if the timeout was set, false if the key didn't exist.
   *
   * @see
   *   [[expire]]
   */
  final def expireAt[K: Schema](key: K, timestamp: Instant): G[Boolean] = {
    val command = RedisCommand(ExpireAt, Tuple2(ArbitraryKeyInput[K](), TimeSecondsInput), BoolOutput)
    command.run((key, timestamp))
  }

  /**
   * Returns all keys matching pattern.
   *
   * @param pattern
   *   string pattern
   * @return
   *   keys matching pattern.
   */
  final def keys(pattern: String): ResultBuilder1[Chunk, G] =
    new ResultBuilder1[Chunk, G] {
      def returning[V: Schema]: G[Chunk[V]] =
        RedisCommand(Keys.Keys, StringInput, ChunkOutput(ArbitraryOutput[V]())).run(pattern)
    }

  /**
   * Atomically transfer a key from a source Redis instance to a destination Redis instance. On success the key is
   * deleted from the original instance and is guaranteed to exist in the target instance.
   *
   * @param host
   *   remote redis host
   * @param port
   *   remote redis instance port
   * @param key
   *   key to be transferred or empty string if using the keys option
   * @param destinationDb
   *   remote database id
   * @param timeout
   *   specifies the longest period without blocking which is allowed during the transfer
   * @param auth
   *   optionally provide password for the remote instance
   * @param copy
   *   copy option, to not remove the key from the local instance
   * @param replace
   *   replace option, to replace existing key on the remote instance
   * @param keys
   *   keys option, to migrate multiple keys, non empty list of keys
   * @return
   *   string OK on success, or NOKEY if no keys were found in the source instance.
   */
  final def migrate[K: Schema](
    host: String,
    port: Long,
    key: K,
    destinationDb: Long,
    timeout: Duration,
    auth: Option[Auth] = None,
    copy: Option[Copy] = None,
    replace: Option[Replace] = None,
    keys: Option[(K, List[K])]
  ): G[String] = {
    val command = RedisCommand(
      Migrate,
      Tuple9(
        StringInput,
        LongInput,
        ArbitraryKeyInput[K](),
        LongInput,
        LongInput,
        OptionalInput(CopyInput),
        OptionalInput(ReplaceInput),
        OptionalInput(AuthInput),
        OptionalInput(NonEmptyList(ArbitraryKeyInput[K]()))
      ),
      StringOutput
    )
    command.run((host, port, key, destinationDb, timeout.toMillis, copy, replace, auth, keys))
  }

  /**
   * Move key from the currently selected database to the specified destination database. When key already exists in the
   * destination database, or it does not exist in the source database, it does nothing.
   *
   * @param key
   *   key
   * @param destinationDb
   *   destination database id
   * @return
   *   true if the key was moved.
   */
  final def move[K: Schema](key: K, destinationDb: Long): G[Boolean] = {
    val command = RedisCommand(Move, Tuple2(ArbitraryKeyInput[K](), LongInput), BoolOutput)
    command.run((key, destinationDb))
  }

  /**
   * Remove the existing timeout on key.
   *
   * @param key
   *   key
   * @return
   *   true if timeout was removed, false if key does not exist or does not have an associated timeout.
   */
  final def persist[K: Schema](key: K): G[Boolean] = {
    val command = RedisCommand(Persist, ArbitraryKeyInput[K](), BoolOutput)
    command.run(key)
  }

  /**
   * Set a timeout on key. After the timeout has expired, the key will automatically be deleted.
   *
   * @param key
   *   key
   * @param timeout
   *   timeout
   * @return
   *   true, if the timeout was set, false if the key didn't exist.
   *
   * @see
   *   [[pExpireAt]]
   */
  final def pExpire[K: Schema](key: K, timeout: Duration): G[Boolean] = {
    val command =
      RedisCommand(PExpire, Tuple2(ArbitraryKeyInput[K](), DurationMillisecondsInput), BoolOutput)
    command.run((key, timeout))
  }

  /**
   * Deletes the key at the specific timestamp. A timestamp in the past will delete the key immediately.
   *
   * @param key
   *   key
   * @param timestamp
   *   an absolute Unix timestamp (milliseconds since January 1, 1970)
   * @return
   *   true, if the timeout was set, false if the key didn't exist.
   *
   * @see
   *   [[pExpire]]
   */
  final def pExpireAt[K: Schema](key: K, timestamp: Instant): G[Boolean] = {
    val command =
      RedisCommand(PExpireAt, Tuple2(ArbitraryKeyInput[K](), TimeMillisecondsInput), BoolOutput)
    command.run((key, timestamp))
  }

  /**
   * Returns the remaining time to live of a key that has a timeout.
   *
   * @param key
   *   key
   * @return
   *   remaining time to live of a key that has a timeout, error otherwise.
   */
  final def pTtl[K: Schema](key: K): G[Duration] = {
    val command = RedisCommand(PTtl, ArbitraryKeyInput[K](), DurationMillisecondsOutput)
    command.run(key)
  }

  /**
   * Return a random key from the currently selected database.
   *
   * @return
   *   key or None when the database is empty.
   */
  final def randomKey: ResultBuilder1[Option, G] =
    new ResultBuilder1[Option, G] {
      def returning[V: Schema]: G[Option[V]] =
        RedisCommand(RandomKey, NoInput, OptionalOutput(ArbitraryOutput[V]())).run(())
    }

  /**
   * Renames key to newKey. It returns an error when key does not exist. If newKey already exists it is overwritten.
   *
   * @param key
   *   key to be renamed
   * @param newKey
   *   new name
   * @return
   *   unit if successful, error otherwise.
   */
  final def rename[K: Schema](key: K, newKey: K): G[Unit] = {
    val command =
      RedisCommand(Rename, Tuple2(ArbitraryKeyInput[K](), ArbitraryKeyInput[K]()), UnitOutput)
    command.run((key, newKey))
  }

  /**
   * Renames key to newKey if newKey does not yet exist. It returns an error when key does not exist.
   *
   * @param key
   *   key to be renamed
   * @param newKey
   *   new name
   * @return
   *   true if key was renamed to newKey, false if newKey already exists.
   */
  final def renameNx[K: Schema](key: K, newKey: K): G[Boolean] = {
    val command =
      RedisCommand(RenameNx, Tuple2(ArbitraryKeyInput[K](), ArbitraryKeyInput[K]()), BoolOutput)
    command.run((key, newKey))
  }

  /**
   * Create a key associated with a value that is obtained by deserializing the provided serialized value. Error when
   * key already exists unless you use the REPLACE option.
   *
   * @param key
   *   key
   * @param ttl
   *   time to live in milliseconds, 0 if without any expire
   * @param value
   *   serialized value
   * @param replace
   *   replace option, replace if existing
   * @param absTtl
   *   absolute ttl option, ttl should represent an absolute Unix timestamp (in milliseconds) in which the key will
   *   expire
   * @param idleTime
   *   idle time based eviction policy
   * @param freq
   *   frequency based eviction policy
   * @return
   *   unit on success.
   */
  final def restore[K: Schema](
    key: K,
    ttl: Long,
    value: Chunk[Byte],
    replace: Option[Replace] = None,
    absTtl: Option[AbsTtl] = None,
    idleTime: Option[IdleTime] = None,
    freq: Option[Freq] = None
  ): G[Unit] = {
    val command = RedisCommand(
      Restore,
      Tuple7(
        ArbitraryKeyInput[K](),
        LongInput,
        ValueInput,
        OptionalInput(ReplaceInput),
        OptionalInput(AbsTtlInput),
        OptionalInput(IdleTimeInput),
        OptionalInput(FreqInput)
      ),
      UnitOutput
    )
    command.run((key, ttl, value, replace, absTtl, idleTime, freq))
  }

  /**
   * Iterates the set of keys in the currently selected Redis database. An iteration starts when the cursor is set to 0,
   * and terminates when the cursor returned by the server is 0.
   *
   * @param cursor
   *   cursor value, starts with zero
   * @param pattern
   *   key pattern
   * @param count
   *   count option, specifies number of returned elements per call
   * @param `type`
   *   type option, filter to only return objects that match a given type
   * @return
   *   returns an updated cursor that the user needs to use as the cursor argument in the next call along with the
   *   values.
   */
  final def scan(
    cursor: Long,
    pattern: Option[String] = None,
    count: Option[Count] = None,
    `type`: Option[RedisType] = None
  ): ResultBuilder1[({ type lambda[x] = (Long, Chunk[x]) })#lambda, G] =
    new ResultBuilder1[({ type lambda[x] = (Long, Chunk[x]) })#lambda, G] {
      def returning[K: Schema]: G[(Long, Chunk[K])] = {
        val command = RedisCommand(
          Scan,
          Tuple4(LongInput, OptionalInput(PatternInput), OptionalInput(CountInput), OptionalInput(RedisTypeInput)),
          ScanOutput(ArbitraryOutput[K]())
        )
        command.run((cursor, pattern.map(Pattern(_)), count, `type`))
      }
    }

  /**
   * Delete all the keys of all the existing databases, not just the currently selected one. This command never fails.
   *
   * @param sync
   *  flushes the databases synchronously
   * @return
   *  Simple string reply: OK.
   */
  final def flushall(sync: Boolean = false): G[Unit] = {
    val command = RedisCommand(
      FlushAll,
      ArbitraryValueInput[String](),
      UnitOutput
    )

    command.run(if (sync) "SYNC" else "ASYNC")
  }

  /**
   * Sorts the list, set, or sorted set stored at key. Returns the sorted elements.
   *
   * @param key
   *   key
   * @param by
   *   by option, specifies a pattern to use as an external key
   * @param limit
   *   limit option, take only limit values, starting at position offset
   * @param order
   *   ordering option, sort descending instead of ascending
   * @param get
   *   get option, return the values referenced by the keys generated from the get patterns
   * @param alpha
   *   alpha option, sort the values alphanumerically, instead of by interpreting the value as floating point number
   * @return
   *   the sorted values, or the values found using the get patterns.
   */
  final def sort[K: Schema](
    key: K,
    by: Option[String] = None,
    limit: Option[Limit] = None,
    order: Order = Order.Ascending,
    get: Option[(String, List[String])] = None,
    alpha: Option[Alpha] = None
  ): ResultBuilder1[Chunk, G] =
    new ResultBuilder1[Chunk, G] {
      def returning[V: Schema]: G[Chunk[V]] = {
        val command = RedisCommand(
          Sort,
          Tuple6(
            ArbitraryKeyInput[K](),
            OptionalInput(ByInput),
            OptionalInput(LimitInput),
            OptionalInput(NonEmptyList(GetInput)),
            OrderInput,
            OptionalInput(AlphaInput)
          ),
          ChunkOutput(ArbitraryOutput[V]())
        )
        command.run((key, by, limit, get, order, alpha))
      }
    }

  /**
   * Sorts the list, set, or sorted set stored at key. Stores the results at storeAt. Returns the number of values
   * sorted.
   *
   * The functions sort and sortStore are both implemented by the Redis command SORT. Because they have different return
   * types, they are split into two Scala functions.
   *
   * @param key
   *   key
   * @param storeAt
   *   where to store the results
   * @param by
   *   by option, specifies a pattern to use as an external key
   * @param limit
   *   limit option, take only limit values, starting at position offset
   * @param order
   *   ordering option, sort descending instead of ascending
   * @param get
   *   get option, return the values referenced by the keys generated from the get patterns
   * @param alpha
   *   alpha option, sort the values alphanumerically, instead of by interpreting the value as floating point number
   * @return
   *   the sorted values, or the values found using the get patterns.
   */
  final def sortStore[K: Schema](
    key: K,
    storeAt: Store,
    by: Option[String] = None,
    limit: Option[Limit] = None,
    order: Order = Order.Ascending,
    get: Option[(String, List[String])] = None,
    alpha: Option[Alpha] = None
  ): G[Long] = {
    val command = RedisCommand(
      Sort,
      Tuple7(
        ArbitraryKeyInput[K](),
        OptionalInput(ByInput),
        OptionalInput(LimitInput),
        OptionalInput(NonEmptyList(GetInput)),
        OrderInput,
        OptionalInput(AlphaInput),
        StoreInput
      ),
      LongOutput
    )
    command.run((key, by, limit, get, order, alpha, storeAt))
  }

  /**
   * Alters the last access time of a key(s). A key is ignored if it does not exist.
   *
   * @param key
   *   one required key
   * @param keys
   *   maybe rest of the keys
   * @return
   *   The number of keys that were touched.
   */
  final def touch[K: Schema](key: K, keys: K*): G[Long] = {
    val command = RedisCommand(Touch, NonEmptyList(ArbitraryKeyInput[K]()), LongOutput)
    command.run((key, keys.toList))
  }

  /**
   * Returns the remaining time to live of a key that has a timeout.
   *
   * @param key
   *   key
   * @return
   *   remaining time to live of a key that has a timeout, error otherwise.
   */
  final def ttl[K: Schema](key: K): G[Duration] = {
    val command = RedisCommand(Ttl, ArbitraryKeyInput[K](), DurationSecondsOutput)
    command.run(key)
  }

  /**
   * Returns the string representation of the type of the value stored at key.
   *
   * @param key
   *   key
   * @return
   *   type of the value stored at key.
   */
  final def typeOf[K: Schema](key: K): G[RedisType] = {
    val command = RedisCommand(TypeOf, ArbitraryKeyInput[K](), TypeOutput)
    command.run(key)
  }

  /**
   * Removes the specified keys. A key is ignored if it does not exist. The command performs the actual memory
   * reclaiming in a different thread, so it is not blocking.
   *
   * @param key
   *   one required key
   * @param keys
   *   maybe rest of the keys
   * @return
   *   The number of keys that were unlinked.
   *
   * @see
   *   [[del]]
   */
  final def unlink[K: Schema](key: K, keys: K*): G[Long] = {
    val command = RedisCommand(Unlink, NonEmptyList(ArbitraryKeyInput[K]()), LongOutput)
    command.run((key, keys.toList))
  }

  /**
   * This command blocks the current client until all the previous write commands are successfully transferred and
   * acknowledged by at least the specified number of replicas.
   *
   * @param replicas
   *   minimum replicas to reach
   * @param timeout
   *   specified as a Duration, 0 means to block forever
   * @return
   *   the number of replicas reached both in case of failure and success.
   */
  final def wait_(replicas: Long, timeout: Duration): G[Long] = {
    val command = RedisCommand(Wait, Tuple2(LongInput, LongInput), LongOutput)
    command.run((replicas, timeout.toMillis))
  }
}

private[redis] object Keys {
  final val Copy      = "COPY"
  final val Del       = "DEL"
  final val Dump      = "DUMP"
  final val Exists    = "EXISTS"
  final val Expire    = "EXPIRE"
  final val ExpireAt  = "EXPIREAT"
  final val Keys      = "KEYS"
  final val Migrate   = "MIGRATE"
  final val Move      = "MOVE"
  final val Persist   = "PERSIST"
  final val PExpire   = "PEXPIRE"
  final val PExpireAt = "PEXPIREAT"
  final val PTtl      = "PTTL"
  final val RandomKey = "RANDOMKEY"
  final val Rename    = "RENAME"
  final val RenameNx  = "RENAMENX"
  final val Restore   = "RESTORE"
  final val Scan      = "SCAN"
  final val Sort      = "SORT"
  final val Touch     = "TOUCH"
  final val Ttl       = "TTL"
  final val TypeOf    = "TYPE"
  final val Unlink    = "UNLINK"
  final val Wait      = "WAIT"
  final val FlushAll  = "FLUSHALL"
}
