package dev.profunktor.valkey4cats

import cats.effect.*
import cats.syntax.all.*
import glide.api.BaseClient
import dev.profunktor.valkey4cats.arguments.{
  FlushMode,
  InfoSection,
  InsertPosition,
  SetOptions,
  ZAddOptions
}
import dev.profunktor.valkey4cats.codec.Codec
import dev.profunktor.valkey4cats.connection.{
  ValkeyClient,
  ValkeyClusterClient,
  ValkeyConnection
}
import dev.profunktor.valkey4cats.effect.FutureLift.FutureLiftOps
import dev.profunktor.valkey4cats.effect.{FutureLift, Log, MkValkey}
import dev.profunktor.valkey4cats.tx.TxRunner

import scala.jdk.CollectionConverters.*

/** Base implementation for Valkey commands
  *
  * Supports both standalone and cluster clients through the ValkeyConnection ADT
  *
  * @param connection Standalone or cluster connection
  * @param keyCodec Codec for encoding/decoding keys
  * @param valueCodec Codec for encoding/decoding values
  * @param tx Transaction runner (stub for Phase 1)
  */
private[valkey4cats] abstract class BaseValkey[F[_]: MkValkey, K, V](
    protected val connection: ValkeyConnection,
    protected val keyCodec: Codec[K],
    protected val valueCodec: Codec[V],
    protected val tx: TxRunner[F]
)(implicit F: Async[F])
    extends ValkeyCommands[F, K, V] {

  /** Get capabilities from MkValkey */
  private implicit val futureLift: FutureLift[F] = MkValkey[F].futureLift
  private implicit val logger: Log[F] = MkValkey[F].log

  /** Get the underlying Glide BaseClient (works for both standalone and cluster) */
  private val baseClient: BaseClient = connection.baseClient

  /** Log errors and re-raise, reducing boilerplate across command methods */
  private def logged[A](cmd: String)(fa: F[A]): F[A] =
    fa.handleErrorWith { err =>
      Log[F].error(s"Error in $cmd: ${err.getMessage}") *>
        Async[F].raiseError(err)
    }

  /** Execute a server command that exists on both GlideClient and GlideClusterClient
    * but not on BaseClient, by dispatching through the connection ADT.
    */
  private def serverCmd[A](cmd: String)(
      standalone: glide.api.GlideClient => F[A],
      cluster: glide.api.GlideClusterClient => F[A]
  ): F[A] =
    logged(cmd) {
      connection match {
        case ValkeyConnection.Standalone(c) => standalone(c.underlying)
        case ValkeyConnection.Clustered(c)  => cluster(c.underlying)
      }
    }

  // ==================== String Commands ====================

  override def get(key: K): F[Option[V]] = logged(s"GET $key") {
    val keyGS = keyCodec.encode(key)
    baseClient
      .get(keyGS)
      .futureLift
      .map(gs => Option(gs).map(valueCodec.decode))
  }

  override def set(key: K, value: V): F[Unit] = logged(s"SET $key") {
    val keyGS = keyCodec.encode(key)
    val valueGS = valueCodec.encode(value)
    baseClient.set(keyGS, valueGS).futureLift.void
  }

  override def set(key: K, value: V, options: SetOptions): F[Option[V]] =
    logged(s"SET $key with options") {
      val keyGS = keyCodec.encode(key)
      val valueGS = valueCodec.encode(value)
      val glideOptions = SetOptions.toGlide(options)
      baseClient
        .set(keyGS, valueGS, glideOptions)
        .futureLift
        .map { result =>
          if (options.returnOldValue && result != null && result != "OK")
            Some(valueCodec.decode(glide.api.models.GlideString.of(result)))
          else None
        }
    }

  override def mGet(keys: Set[K]): F[Map[K, V]] = {
    if (keys.isEmpty) Async[F].pure(Map.empty[K, V])
    else
      logged("MGET") {
        val keysList = keys.toList
        val keysArray = keysList.map(k => keyCodec.encode(k)).toArray
        baseClient
          .mget(keysArray)
          .futureLift
          .map { javaArray =>
            keysList
              .zip(javaArray.toList)
              .collect {
                case (key, value) if value != null =>
                  key -> valueCodec.decode(value)
              }
              .toMap
          }
      }
  }

  override def mSet(keyValues: Map[K, V]): F[Unit] = {
    if (keyValues.isEmpty) Async[F].unit
    else
      logged("MSET") {
        val javaMap = keyValues.map { case (k, v) =>
          new String(keyCodec.encode(k).getBytes()) -> new String(
            valueCodec.encode(v).getBytes()
          )
        }.asJava
        baseClient.mset(javaMap).futureLift.void
      }
  }

  override def incr(key: K): F[Long] = logged(s"INCR $key") {
    baseClient.incr(keyCodec.encode(key)).futureLift.map(_.longValue())
  }

  override def incrBy(key: K, amount: Long): F[Long] =
    logged(s"INCRBY $key $amount") {
      baseClient
        .incrBy(keyCodec.encode(key), amount)
        .futureLift
        .map(_.longValue())
    }

  override def decr(key: K): F[Long] = logged(s"DECR $key") {
    baseClient.decr(keyCodec.encode(key)).futureLift.map(_.longValue())
  }

  override def decrBy(key: K, amount: Long): F[Long] =
    logged(s"DECRBY $key $amount") {
      baseClient
        .decrBy(keyCodec.encode(key), amount)
        .futureLift
        .map(_.longValue())
    }

  override def append(key: K, value: V): F[Long] = logged(s"APPEND $key") {
    baseClient
      .append(keyCodec.encode(key), valueCodec.encode(value))
      .futureLift
      .map(_.longValue())
  }

  override def strlen(key: K): F[Long] = logged(s"STRLEN $key") {
    baseClient.strlen(keyCodec.encode(key)).futureLift.map(_.longValue())
  }

  // ==================== Key Commands ====================

  override def del(keys: K*): F[Long] = {
    if (keys.isEmpty) Async[F].pure(0L)
    else
      logged("DEL") {
        baseClient
          .del(keys.map(keyCodec.encode).toArray)
          .futureLift
          .map(_.longValue())
      }
  }

  override def exists(key: K): F[Boolean] = logged(s"EXISTS $key") {
    baseClient
      .exists(Array(keyCodec.encode(key)))
      .futureLift
      .map(_.longValue() == 1L)
  }

  override def existsMany(keys: K*): F[Long] = {
    if (keys.isEmpty) Async[F].pure(0L)
    else
      logged("EXISTS") {
        baseClient
          .exists(keys.map(keyCodec.encode).toArray)
          .futureLift
          .map(_.longValue())
      }
  }

  // ==================== Hash Commands ====================

  override def hset(key: K, fieldValues: Map[K, V]): F[Long] = {
    if (fieldValues.isEmpty) Async[F].pure(0L)
    else
      logged(s"HSET $key") {
        val javaMap = fieldValues.map { case (k, v) =>
          keyCodec.encode(k) -> valueCodec.encode(v)
        }.asJava
        baseClient
          .hset(keyCodec.encode(key), javaMap)
          .futureLift
          .map(_.longValue())
      }
  }

  override def hget(key: K, field: K): F[Option[V]] =
    logged(s"HGET $key $field") {
      baseClient
        .hget(keyCodec.encode(key), keyCodec.encode(field))
        .futureLift
        .map(gs => Option(gs).map(valueCodec.decode))
    }

  override def hgetall(key: K): F[Map[K, V]] = logged(s"HGETALL $key") {
    baseClient
      .hgetall(keyCodec.encode(key))
      .futureLift
      .map(
        _.asScala
          .map { case (k, v) =>
            keyCodec.decode(k) -> valueCodec.decode(v)
          }
          .toMap
      )
  }

  override def hmget(key: K, fields: K*): F[List[Option[V]]] = {
    if (fields.isEmpty) Async[F].pure(List.empty)
    else
      logged(s"HMGET $key") {
        baseClient
          .hmget(keyCodec.encode(key), fields.map(keyCodec.encode).toArray)
          .futureLift
          .map(_.toList.map(gs => Option(gs).map(valueCodec.decode)))
      }
  }

  override def hdel(key: K, fields: K*): F[Long] = {
    if (fields.isEmpty) Async[F].pure(0L)
    else
      logged(s"HDEL $key") {
        baseClient
          .hdel(keyCodec.encode(key), fields.map(keyCodec.encode).toArray)
          .futureLift
          .map(_.longValue())
      }
  }

  override def hexists(key: K, field: K): F[Boolean] =
    logged(s"HEXISTS $key $field") {
      baseClient
        .hexists(keyCodec.encode(key), keyCodec.encode(field))
        .futureLift
        .map(_.booleanValue())
    }

  override def hkeys(key: K): F[List[K]] = logged(s"HKEYS $key") {
    baseClient
      .hkeys(keyCodec.encode(key))
      .futureLift
      .map(_.toList.map(keyCodec.decode))
  }

  override def hvals(key: K): F[List[V]] = logged(s"HVALS $key") {
    baseClient
      .hvals(keyCodec.encode(key))
      .futureLift
      .map(_.toList.map(valueCodec.decode))
  }

  override def hlen(key: K): F[Long] = logged(s"HLEN $key") {
    baseClient.hlen(keyCodec.encode(key)).futureLift.map(_.longValue())
  }

  override def hincrBy(key: K, field: K, increment: Long): F[Long] =
    logged(s"HINCRBY $key $field $increment") {
      baseClient
        .hincrBy(keyCodec.encode(key), keyCodec.encode(field), increment)
        .futureLift
        .map(_.longValue())
    }

  override def hincrByFloat(key: K, field: K, increment: Double): F[Double] =
    logged(s"HINCRBYFLOAT $key $field $increment") {
      baseClient
        .hincrByFloat(keyCodec.encode(key), keyCodec.encode(field), increment)
        .futureLift
        .map(_.doubleValue())
    }

  override def hsetnx(key: K, field: K, value: V): F[Boolean] =
    logged(s"HSETNX $key $field") {
      baseClient
        .hsetnx(
          keyCodec.encode(key),
          keyCodec.encode(field),
          valueCodec.encode(value)
        )
        .futureLift
        .map(_.booleanValue())
    }

  override def hstrlen(key: K, field: K): F[Long] =
    logged(s"HSTRLEN $key $field") {
      baseClient
        .hstrlen(keyCodec.encode(key), keyCodec.encode(field))
        .futureLift
        .map(_.longValue())
    }

  override def hrandfield(key: K): F[Option[K]] = logged(s"HRANDFIELD $key") {
    baseClient
      .hrandfield(keyCodec.encode(key))
      .futureLift
      .map(gs => Option(gs).map(keyCodec.decode))
  }

  override def hrandfieldWithCount(key: K, count: Long): F[List[K]] =
    logged(s"HRANDFIELDWITHCOUNT $key $count") {
      baseClient
        .hrandfieldWithCount(keyCodec.encode(key), count)
        .futureLift
        .map(_.toList.map(keyCodec.decode))
    }

  override def hrandfieldWithCountWithValues(
      key: K,
      count: Long
  ): F[List[(K, V)]] =
    logged(s"HRANDFIELDWITHCOUNTWITHVALUES $key $count") {
      baseClient
        .hrandfieldWithCountWithValues(keyCodec.encode(key), count)
        .futureLift
        .map(
          _.toList
            .map(pair => keyCodec.decode(pair(0)) -> valueCodec.decode(pair(1)))
        )
    }

  // ==================== List Commands ====================

  override def lpush(key: K, elements: V*): F[Long] = {
    if (elements.isEmpty) Async[F].pure(0L)
    else
      logged(s"LPUSH $key") {
        baseClient
          .lpush(keyCodec.encode(key), elements.map(valueCodec.encode).toArray)
          .futureLift
          .map(_.longValue())
      }
  }

  override def rpush(key: K, elements: V*): F[Long] = {
    if (elements.isEmpty) Async[F].pure(0L)
    else
      logged(s"RPUSH $key") {
        baseClient
          .rpush(keyCodec.encode(key), elements.map(valueCodec.encode).toArray)
          .futureLift
          .map(_.longValue())
      }
  }

  override def lpop(key: K): F[Option[V]] = logged(s"LPOP $key") {
    baseClient
      .lpop(keyCodec.encode(key))
      .futureLift
      .map(gs => Option(gs).map(valueCodec.decode))
  }

  override def rpop(key: K): F[Option[V]] = logged(s"RPOP $key") {
    baseClient
      .rpop(keyCodec.encode(key))
      .futureLift
      .map(gs => Option(gs).map(valueCodec.decode))
  }

  override def lpopCount(key: K, count: Long): F[List[V]] =
    logged(s"LPOPCOUNT $key $count") {
      baseClient
        .lpopCount(keyCodec.encode(key), count)
        .futureLift
        .map(arr =>
          if (arr == null) List.empty else arr.toList.map(valueCodec.decode)
        )
    }

  override def rpopCount(key: K, count: Long): F[List[V]] =
    logged(s"RPOPCOUNT $key $count") {
      baseClient
        .rpopCount(keyCodec.encode(key), count)
        .futureLift
        .map(arr =>
          if (arr == null) List.empty else arr.toList.map(valueCodec.decode)
        )
    }

  override def lrange(key: K, start: Long, stop: Long): F[List[V]] =
    logged(s"LRANGE $key $start $stop") {
      baseClient
        .lrange(keyCodec.encode(key), start, stop)
        .futureLift
        .map(_.toList.map(valueCodec.decode))
    }

  override def lindex(key: K, index: Long): F[Option[V]] =
    logged(s"LINDEX $key $index") {
      baseClient
        .lindex(keyCodec.encode(key), index)
        .futureLift
        .map(gs => Option(gs).map(valueCodec.decode))
    }

  override def llen(key: K): F[Long] = logged(s"LLEN $key") {
    baseClient.llen(keyCodec.encode(key)).futureLift.map(_.longValue())
  }

  override def ltrim(key: K, start: Long, stop: Long): F[Unit] =
    logged(s"LTRIM $key $start $stop") {
      baseClient.ltrim(keyCodec.encode(key), start, stop).futureLift.void
    }

  override def lset(key: K, index: Long, element: V): F[Unit] =
    logged(s"LSET $key $index") {
      baseClient
        .lset(keyCodec.encode(key), index, valueCodec.encode(element))
        .futureLift
        .void
    }

  override def lrem(key: K, count: Long, element: V): F[Long] =
    logged(s"LREM $key $count") {
      baseClient
        .lrem(keyCodec.encode(key), count, valueCodec.encode(element))
        .futureLift
        .map(_.longValue())
    }

  override def linsert(
      key: K,
      position: InsertPosition,
      pivot: V,
      element: V
  ): F[Long] = logged(s"LINSERT $key") {
    baseClient
      .linsert(
        keyCodec.encode(key),
        position.toGlide,
        valueCodec.encode(pivot),
        valueCodec.encode(element)
      )
      .futureLift
      .map(_.longValue())
  }

  override def lpos(key: K, element: V): F[Option[Long]] =
    logged(s"LPOS $key") {
      baseClient
        .lpos(keyCodec.encode(key), valueCodec.encode(element))
        .futureLift
        .map(result => Option(result).map(_.longValue()))
    }

  // ==================== Set Commands ====================

  override def sadd(key: K, members: V*): F[Long] = {
    if (members.isEmpty) Async[F].pure(0L)
    else
      logged(s"SADD $key") {
        baseClient
          .sadd(keyCodec.encode(key), members.map(valueCodec.encode).toArray)
          .futureLift
          .map(_.longValue())
      }
  }

  override def srem(key: K, members: V*): F[Long] = {
    if (members.isEmpty) Async[F].pure(0L)
    else
      logged(s"SREM $key") {
        baseClient
          .srem(keyCodec.encode(key), members.map(valueCodec.encode).toArray)
          .futureLift
          .map(_.longValue())
      }
  }

  override def smembers(key: K): F[Set[V]] = logged(s"SMEMBERS $key") {
    baseClient
      .smembers(keyCodec.encode(key))
      .futureLift
      .map(_.asScala.map(valueCodec.decode).toSet)
  }

  override def sismember(key: K, member: V): F[Boolean] =
    logged(s"SISMEMBER $key") {
      baseClient
        .sismember(keyCodec.encode(key), valueCodec.encode(member))
        .futureLift
        .map(_.booleanValue())
    }

  override def smismember(key: K, members: V*): F[List[Boolean]] = {
    if (members.isEmpty) Async[F].pure(List.empty[Boolean])
    else
      logged(s"SMISMEMBER $key") {
        baseClient
          .smismember(
            keyCodec.encode(key),
            members.map(valueCodec.encode).toArray
          )
          .futureLift
          .map(_.map(_.booleanValue()).toList)
      }
  }

  override def scard(key: K): F[Long] = logged(s"SCARD $key") {
    baseClient.scard(keyCodec.encode(key)).futureLift.map(_.longValue())
  }

  override def sunion(keys: K*): F[Set[V]] = {
    if (keys.isEmpty) Async[F].pure(Set.empty[V])
    else
      logged("SUNION") {
        baseClient
          .sunion(keys.map(keyCodec.encode).toArray)
          .futureLift
          .map(_.asScala.map(valueCodec.decode).toSet)
      }
  }

  override def sunionstore(destination: K, keys: K*): F[Long] = {
    if (keys.isEmpty) Async[F].pure(0L)
    else
      logged(s"SUNIONSTORE $destination") {
        baseClient
          .sunionstore(
            keyCodec.encode(destination),
            keys.map(keyCodec.encode).toArray
          )
          .futureLift
          .map(_.longValue())
      }
  }

  override def sinter(keys: K*): F[Set[V]] = {
    if (keys.isEmpty) Async[F].pure(Set.empty[V])
    else
      logged("SINTER") {
        baseClient
          .sinter(keys.map(keyCodec.encode).toArray)
          .futureLift
          .map(_.asScala.map(valueCodec.decode).toSet)
      }
  }

  override def sinterstore(destination: K, keys: K*): F[Long] = {
    if (keys.isEmpty) Async[F].pure(0L)
    else
      logged(s"SINTERSTORE $destination") {
        baseClient
          .sinterstore(
            keyCodec.encode(destination),
            keys.map(keyCodec.encode).toArray
          )
          .futureLift
          .map(_.longValue())
      }
  }

  override def sdiff(keys: K*): F[Set[V]] = {
    if (keys.isEmpty) Async[F].pure(Set.empty[V])
    else
      logged("SDIFF") {
        baseClient
          .sdiff(keys.map(keyCodec.encode).toArray)
          .futureLift
          .map(_.asScala.map(valueCodec.decode).toSet)
      }
  }

  override def sdiffstore(destination: K, keys: K*): F[Long] = {
    if (keys.isEmpty) Async[F].pure(0L)
    else
      logged(s"SDIFFSTORE $destination") {
        baseClient
          .sdiffstore(
            keyCodec.encode(destination),
            keys.map(keyCodec.encode).toArray
          )
          .futureLift
          .map(_.longValue())
      }
  }

  override def spop(key: K): F[Option[V]] = logged(s"SPOP $key") {
    baseClient
      .spop(keyCodec.encode(key))
      .futureLift
      .map(result => Option(result).map(valueCodec.decode))
  }

  override def spopCount(key: K, count: Long): F[Set[V]] =
    logged(s"SPOP $key $count") {
      baseClient
        .spopCount(keyCodec.encode(key), count)
        .futureLift
        .map(_.asScala.map(valueCodec.decode).toSet)
    }

  override def srandmember(key: K): F[Option[V]] =
    logged(s"SRANDMEMBER $key") {
      baseClient
        .srandmember(keyCodec.encode(key))
        .futureLift
        .map(result => Option(result).map(valueCodec.decode))
    }

  override def srandmemberCount(key: K, count: Long): F[List[V]] =
    logged(s"SRANDMEMBER $key $count") {
      baseClient
        .srandmember(keyCodec.encode(key), count)
        .futureLift
        .map(_.toList.map(valueCodec.decode))
    }

  override def smove(source: K, destination: K, member: V): F[Boolean] =
    logged(s"SMOVE $source $destination") {
      baseClient
        .smove(
          keyCodec.encode(source),
          keyCodec.encode(destination),
          valueCodec.encode(member)
        )
        .futureLift
        .map(_.booleanValue())
    }

  // ==================== Sorted Set Commands ====================

  override def zadd(key: K, membersScores: Map[V, Double]): F[Long] = {
    if (membersScores.isEmpty) Async[F].pure(0L)
    else
      logged(s"ZADD $key") {
        val javaMap = membersScores.map { case (member, score) =>
          valueCodec.encode(member) -> Double.box(score)
        }.asJava
        baseClient
          .zadd(keyCodec.encode(key), javaMap)
          .futureLift
          .map(_.longValue())
      }
  }

  override def zadd(
      key: K,
      membersScores: Map[V, Double],
      options: ZAddOptions
  ): F[Long] = {
    if (membersScores.isEmpty) Async[F].pure(0L)
    else
      logged(s"ZADD $key with options") {
        val javaMap = membersScores.map { case (member, score) =>
          valueCodec.encode(member) -> Double.box(score)
        }.asJava
        baseClient
          .zadd(keyCodec.encode(key), javaMap, ZAddOptions.toGlide(options))
          .futureLift
          .map(_.longValue())
      }
  }

  override def zrem(key: K, members: V*): F[Long] = {
    if (members.isEmpty) Async[F].pure(0L)
    else
      logged(s"ZREM $key") {
        baseClient
          .zrem(keyCodec.encode(key), members.map(valueCodec.encode).toArray)
          .futureLift
          .map(_.longValue())
      }
  }

  override def zrange(key: K, start: Long, stop: Long): F[List[V]] =
    logged(s"ZRANGE $key $start $stop") {
      val rangeQuery =
        new glide.api.models.commands.RangeOptions.RangeByIndex(start, stop)
      baseClient
        .zrange(keyCodec.encode(key), rangeQuery)
        .futureLift
        .map(_.toList.map(valueCodec.decode))
    }

  override def zrangeWithScores(
      key: K,
      start: Long,
      stop: Long
  ): F[List[(V, Double)]] =
    logged(s"ZRANGE (with scores) $key $start $stop") {
      val rangeQuery =
        new glide.api.models.commands.RangeOptions.RangeByIndex(start, stop)
      baseClient
        .zrangeWithScores(keyCodec.encode(key), rangeQuery)
        .futureLift
        .map(_.asScala.toList.map { case (gs, score) =>
          (valueCodec.decode(gs), score.doubleValue())
        })
    }

  override def zscore(key: K, member: V): F[Option[Double]] =
    logged(s"ZSCORE $key") {
      baseClient
        .zscore(keyCodec.encode(key), valueCodec.encode(member))
        .futureLift
        .map(result => Option(result).map(_.doubleValue()))
    }

  override def zmscore(key: K, members: V*): F[List[Option[Double]]] = {
    if (members.isEmpty) Async[F].pure(List.empty[Option[Double]])
    else
      logged(s"ZMSCORE $key") {
        baseClient
          .zmscore(keyCodec.encode(key), members.map(valueCodec.encode).toArray)
          .futureLift
          .map(_.toList.map(score => Option(score).map(_.doubleValue())))
      }
  }

  override def zcard(key: K): F[Long] = logged(s"ZCARD $key") {
    baseClient.zcard(keyCodec.encode(key)).futureLift.map(_.longValue())
  }

  override def zrank(key: K, member: V): F[Option[Long]] =
    logged(s"ZRANK $key") {
      baseClient
        .zrank(keyCodec.encode(key), valueCodec.encode(member))
        .futureLift
        .map(result => Option(result).map(_.longValue()))
    }

  override def zrevrank(key: K, member: V): F[Option[Long]] =
    logged(s"ZREVRANK $key") {
      baseClient
        .zrevrank(keyCodec.encode(key), valueCodec.encode(member))
        .futureLift
        .map(result => Option(result).map(_.longValue()))
    }

  override def zincrby(key: K, increment: Double, member: V): F[Double] =
    logged(s"ZINCRBY $key $increment") {
      baseClient
        .zincrby(keyCodec.encode(key), increment, valueCodec.encode(member))
        .futureLift
        .map(_.doubleValue())
    }

  override def zcount(key: K, min: Double, max: Double): F[Long] =
    logged(s"ZCOUNT $key $min $max") {
      val minScore =
        new glide.api.models.commands.RangeOptions.ScoreBoundary(min, true)
      val maxScore =
        new glide.api.models.commands.RangeOptions.ScoreBoundary(max, true)
      baseClient
        .zcount(keyCodec.encode(key), minScore, maxScore)
        .futureLift
        .map(_.longValue())
    }

  private def decodeScoreMap(
      javaMap: java.util.Map[glide.api.models.GlideString, java.lang.Double]
  ): List[(V, Double)] =
    javaMap.asScala.toList.map { case (gs, score) =>
      (valueCodec.decode(gs), score.doubleValue())
    }

  override def zpopmin(key: K): F[Option[(V, Double)]] =
    logged(s"ZPOPMIN $key") {
      baseClient
        .zpopmin(keyCodec.encode(key))
        .futureLift
        .map(_.asScala.headOption.map { case (gs, score) =>
          (valueCodec.decode(gs), score.doubleValue())
        })
    }

  override def zpopminCount(key: K, count: Long): F[List[(V, Double)]] =
    logged(s"ZPOPMIN $key $count") {
      baseClient
        .zpopmin(keyCodec.encode(key), count)
        .futureLift
        .map(decodeScoreMap)
    }

  override def zpopmax(key: K): F[Option[(V, Double)]] =
    logged(s"ZPOPMAX $key") {
      baseClient
        .zpopmax(keyCodec.encode(key))
        .futureLift
        .map(_.asScala.headOption.map { case (gs, score) =>
          (valueCodec.decode(gs), score.doubleValue())
        })
    }

  override def zpopmaxCount(key: K, count: Long): F[List[(V, Double)]] =
    logged(s"ZPOPMAX $key $count") {
      baseClient
        .zpopmax(keyCodec.encode(key), count)
        .futureLift
        .map(decodeScoreMap)
    }

  override def zrandmember(key: K): F[Option[V]] = logged(s"ZRANDMEMBER $key") {
    baseClient
      .zrandmember(keyCodec.encode(key))
      .futureLift
      .map(result => Option(result).map(valueCodec.decode))
  }

  override def zrandmemberCount(key: K, count: Long): F[List[V]] =
    logged(s"ZRANDMEMBER $key $count") {
      baseClient
        .zrandmemberWithCount(keyCodec.encode(key), count)
        .futureLift
        .map(_.toList.map(valueCodec.decode))
    }

  override def zrandmemberWithScores(
      key: K,
      count: Long
  ): F[List[(V, Double)]] =
    logged(s"ZRANDMEMBER (with scores) $key $count") {
      baseClient
        .zrandmemberWithCountWithScores(keyCodec.encode(key), count)
        .futureLift
        .map(_.toList.map { pair =>
          val gs = pair(0).asInstanceOf[glide.api.models.GlideString]
          val score = pair(1).asInstanceOf[java.lang.Double]
          (valueCodec.decode(gs), score.doubleValue())
        })
    }

  // ==================== Server Management Commands ====================

  override def info: F[String] =
    serverCmd("INFO")(
      _.info().futureLift,
      _.info().futureLift.map(_.getSingleValue)
    )

  override def info(sections: Set[InfoSection]): F[String] = {
    val sectionsArray = InfoSection.toGlideArray(sections)
    serverCmd("INFO with sections")(
      _.info(sectionsArray).futureLift,
      _.info(sectionsArray).futureLift.map(_.getSingleValue)
    )
  }

  override def configRewrite: F[String] =
    serverCmd("CONFIG REWRITE")(
      _.configRewrite().futureLift,
      _.configRewrite().futureLift
    )

  override def configResetStat: F[String] =
    serverCmd("CONFIG RESETSTAT")(
      _.configResetStat().futureLift,
      _.configResetStat().futureLift
    )

  override def configGet(parameters: Set[String]): F[Map[String, String]] = {
    val paramsArray = parameters.toArray
    serverCmd("CONFIG GET")(
      _.configGet(paramsArray).futureLift.map(_.asScala.toMap),
      _.configGet(paramsArray).futureLift.map(_.asScala.toMap)
    )
  }

  override def configSet(parameters: Map[String, String]): F[String] = {
    val javaMap = parameters.asJava
    serverCmd("CONFIG SET")(
      _.configSet(javaMap).futureLift,
      _.configSet(javaMap).futureLift
    )
  }

  private def parseTime(array: Array[String]): (Long, Long) = {
    val seconds = array(0).toLong
    val microseconds = array(1).toLong
    (seconds, microseconds)
  }

  override def time: F[(Long, Long)] =
    serverCmd("TIME")(
      _.time().futureLift.map(parseTime),
      _.time().futureLift.map(parseTime)
    )

  override def lastSave: F[Long] =
    serverCmd("LASTSAVE")(
      _.lastsave().futureLift.map(_.longValue()),
      _.lastsave().futureLift.map(_.longValue())
    )

  override def flushAll: F[String] =
    serverCmd("FLUSHALL")(
      _.flushall().futureLift,
      _.flushall().futureLift
    )

  override def flushAll(mode: FlushMode): F[String] = {
    val glideMode = FlushMode.toGlide(mode)
    serverCmd(s"FLUSHALL with mode $mode")(
      _.flushall(glideMode).futureLift,
      _.flushall(glideMode).futureLift
    )
  }

  override def flushDB: F[String] =
    serverCmd("FLUSHDB")(
      _.flushdb().futureLift,
      _.flushdb().futureLift
    )

  override def flushDB(mode: FlushMode): F[String] = {
    val glideMode = FlushMode.toGlide(mode)
    serverCmd(s"FLUSHDB with mode $mode")(
      _.flushdb(glideMode).futureLift,
      _.flushdb(glideMode).futureLift
    )
  }

  override def lolwut: F[String] =
    serverCmd("LOLWUT")(
      _.lolwut().futureLift,
      _.lolwut().futureLift
    )

  override def lolwut(version: Int): F[String] =
    serverCmd(s"LOLWUT version $version")(
      _.lolwut(version).futureLift,
      _.lolwut(version).futureLift
    )

  override def lolwut(version: Int, parameters: List[Int]): F[String] = {
    val paramsArray = parameters.toArray
    serverCmd(s"LOLWUT version $version")(
      _.lolwut(version, paramsArray).futureLift,
      _.lolwut(version, paramsArray).futureLift
    )
  }

  override def dbSize: F[Long] =
    serverCmd("DBSIZE")(
      _.dbsize().futureLift.map(_.longValue()),
      _.dbsize().futureLift.map(_.longValue())
    )
}

/** Standalone client commands implementation */
private[valkey4cats] class ValkeyStandalone[F[_]: MkValkey: Async, K, V](
    client: ValkeyClient,
    keyCodec: Codec[K],
    valueCodec: Codec[V],
    tx: TxRunner[F]
) extends BaseValkey[F, K, V](
      ValkeyConnection.Standalone(client),
      keyCodec,
      valueCodec,
      tx
    )

/** Cluster client commands implementation */
private[valkey4cats] class ValkeyCluster[F[_]: MkValkey: Async, K, V](
    client: ValkeyClusterClient,
    keyCodec: Codec[K],
    valueCodec: Codec[V],
    tx: TxRunner[F]
) extends BaseValkey[F, K, V](
      ValkeyConnection.Clustered(client),
      keyCodec,
      valueCodec,
      tx
    )
