package zio.redis.options

import zio.Chunk

trait Server {

  sealed trait ArrayEntry[+A]

  object ArrayEntry {

    final case class Entry[A](value: A)                    extends ArrayEntry[A]
    final case class Array[A](value: Chunk[ArrayEntry[A]]) extends ArrayEntry[A]
    def empty[A] = Array(Chunk.empty[ArrayEntry[A]])
  }

  final case class LogEntry(
    count: Long,
    reason: String,
    context: String,
    `object`: String,
    username: String,
    ageSeconds: String,
    clientInfo: String,
    entryId: Long,
    timestampCreated: Long,
    timestampLastUpdated: Long
  )

  final case class CommandEntry(
    name: String,
    arity: Long,
    flags: Chunk[String],
    firstKey: Long,
    lastKey: Long,
    step: Long,
    aclCategories: Chunk[String],
    tips: Chunk[String],
    keySpecifications: Chunk[CommandKeySpec],
    subcommands: Chunk[String]
  )

  final case class CommandKeySpec(
    beginSearch: BeginSearch,
    findKeys: String,
    flags: Chunk[String],
    notes: Chunk[String]
  )

  final case class BeginSearch(`type`: BeginSearchType, spec: BeginSearchSpec)

  final case class FindKeys(`type`: FindKeysType, spec: FindKeysSpec)
  sealed trait BeginSearchType

  object BeginSearchType {
    case object IndexType extends BeginSearchType
    case object KeywordType extends BeginSearchType
    case object UnknownType extends BeginSearchType
  }

  sealed trait BeginSearchSpec

  object BeginSearchSpec {
    final case class IndexSpec(value: Long) extends BeginSearchSpec
    final case class KeyWordSpec(keyWord: String, startFrom: Long) extends BeginSearchSpec
    case object UnknownSpec extends BeginSearchSpec
  }

  sealed trait FindKeysType

  object FindKeysType {
    case object RangeType extends FindKeysType
    case object KeyNumType extends FindKeysType
    case object UnknownType extends FindKeysType
  }

  sealed trait FindKeysSpec

  object FindKeysSpec {
    final case class RangeSpec(lastKey: Long, keyStep: Long, limit: Long) extends FindKeysSpec
    final case class KeyNumSpec(keyNumIdx: Long, firstKey: Long, keyStep: Long) extends FindKeysSpec
    case object UnknownSpec extends FindKeysSpec
  }
}
