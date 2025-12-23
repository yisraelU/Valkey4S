package dev.profunktor.valkey4cats

import cats.effect.*
import cats.syntax.all.*
import glide.api.BaseClient
import dev.profunktor.valkey4cats.arguments.{InsertPosition, SetOptions, ZAddOptions}
import dev.profunktor.valkey4cats.codec.Codec
import dev.profunktor.valkey4cats.connection.{ValkeyClient, ValkeyClusterClient}
import dev.profunktor.valkey4cats.effect.FutureLift.FutureLiftOps
import dev.profunktor.valkey4cats.effect.{FutureLift, Log, MkValkey}
import dev.profunktor.valkey4cats.tx.TxRunner

import scala.jdk.CollectionConverters.*

/** Base implementation for Valkey commands
  *
  * Supports both standalone and cluster clients through the BaseClient abstraction
  *
  * @param client Either standalone or cluster client
  * @param keyCodec Codec for encoding/decoding keys
  * @param valueCodec Codec for encoding/decoding values
  * @param tx Transaction runner (stub for Phase 1)
  */
private[valkey4cats] abstract class BaseValkey[F[_]: MkValkey, K, V](
    protected val client: Either[ValkeyClient, ValkeyClusterClient],
    protected val keyCodec: Codec[K],
    protected val valueCodec: Codec[V],
    protected val tx: TxRunner[F]
)(implicit F: Async[F])
    extends ValkeyCommands[F, K, V] {

  /** Get capabilities from MkValkey */
  private implicit val futureLift: FutureLift[F] = MkValkey[F].futureLift
  private implicit val logger: Log[F] = MkValkey[F].log

  /** Get the underlying Glide BaseClient (works for both standalone and cluster) */
  private val baseClient: BaseClient = client match {
    case Left(standalone) => standalone.underlying
    case Right(cluster)   => cluster.underlying
  }

  // ==================== String Commands ====================

  override def get(key: K): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.get(keyGS).futureLift
      .map(gs => Option(gs).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in GET $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def set(key: K, value: V): F[Unit] = {
    val keyGS = keyCodec.encode(key)
    val valueGS = valueCodec.encode(value)
    baseClient.set(keyGS, valueGS).futureLift
      .void
      .handleErrorWith { err =>
        Log[F].error(s"Error in SET $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def set(key: K, value: V, options: SetOptions): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    val valueGS = valueCodec.encode(value)
    val glideOptions = SetOptions.toGlide(options)

    baseClient.set(keyGS, valueGS, glideOptions).futureLift
      .map { result =>
        if (options.returnOldValue && result != null && result != "OK") {
          Some(valueCodec.decode(glide.api.models.GlideString.of(result)))
        } else {
          None
        }
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in SET $key with options: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def mGet(keys: Set[K]): F[Map[K, V]] = {
    if (keys.isEmpty) {
      Async[F].pure(Map.empty[K, V])
    } else {
      val keysList = keys.toList
      val keysArray = keysList.map(k => keyCodec.encode(k)).toArray
      baseClient.mget(keysArray).futureLift
        .map { javaArray =>
          keysList
            .zip(javaArray.toList)
            .collect {
              case (key, value) if value != null =>
                key -> valueCodec.decode(value)
            }
            .toMap
        }
        .handleErrorWith { err =>
          Log[F].error(s"Error in MGET: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def mSet(keyValues: Map[K, V]): F[Unit] = {
    if (keyValues.isEmpty) {
      Async[F].unit
    } else {
      // Glide expects Map[String, String], need to convert GlideString to String
      val javaMap = keyValues.map { case (k, v) =>
        new String(keyCodec.encode(k).getBytes()) -> new String(
          valueCodec.encode(v).getBytes()
        )
      }.asJava

      baseClient.mset(javaMap).futureLift
        .void
        .handleErrorWith { err =>
          Log[F].error(s"Error in MSET: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def incr(key: K): F[Long] = {
    val keyGS = keyCodec.encode(key)
    baseClient.incr(keyGS).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in INCR $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def incrBy(key: K, amount: Long): F[Long] = {
    val keyGS = keyCodec.encode(key)
    baseClient.incrBy(keyGS, amount).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in INCRBY $key $amount: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def decr(key: K): F[Long] = {
    val keyGS = keyCodec.encode(key)
    baseClient.decr(keyGS).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in DECR $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def decrBy(key: K, amount: Long): F[Long] = {
    val keyGS = keyCodec.encode(key)
    baseClient.decrBy(keyGS, amount).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in DECRBY $key $amount: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def append(key: K, value: V): F[Long] = {
    val keyGS = keyCodec.encode(key)
    val valueGS = valueCodec.encode(value)
    baseClient.append(keyGS, valueGS).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in APPEND $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def strlen(key: K): F[Long] = {
    val keyGS = keyCodec.encode(key)
    baseClient.strlen(keyGS).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in STRLEN $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  // ==================== Key Commands ====================

  override def del(keys: K*): F[Long] = {
    if (keys.isEmpty) {
      Async[F].pure(0L)
    } else {
      val keysArray = keys.map(keyCodec.encode).toArray
      baseClient.del(keysArray).futureLift
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in DEL: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def exists(key: K): F[Boolean] = {
    val keysArray = Array(keyCodec.encode(key))
    baseClient.exists(keysArray).futureLift
      .map(_.longValue() == 1L)
      .handleErrorWith { err =>
        Log[F].error(s"Error in EXISTS $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def existsMany(keys: K*): F[Long] = {
    if (keys.isEmpty) {
      Async[F].pure(0L)
    } else {
      val keysArray = keys.map(keyCodec.encode).toArray
      baseClient.exists(keysArray).futureLift
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in EXISTS: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  // ==================== Hash Commands ====================

  override def hset(key: K, fieldValues: Map[K, V]): F[Long] = {
    if (fieldValues.isEmpty) {
      Async[F].pure(0L)
    } else {
      val keyGS = keyCodec.encode(key)
      val javaMap = fieldValues.map { case (k, v) =>
        keyCodec.encode(k) -> valueCodec.encode(v)
      }.asJava

      baseClient.hset(keyGS, javaMap).futureLift
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in HSET $key: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def hget(key: K, field: K): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    val fieldGS = keyCodec.encode(field)
    baseClient.hget(keyGS, fieldGS).futureLift
      .map(gs => Option(gs).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in HGET $key $field: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hgetall(key: K): F[Map[K, V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.hgetall(keyGS).futureLift
      .map { javaMap =>
        javaMap.asScala.map { case (k, v) =>
          keyCodec.decode(k) -> valueCodec.decode(v)
        }.toMap
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in HGETALL $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hmget(key: K, fields: K*): F[List[Option[V]]] = {
    if (fields.isEmpty) {
      Async[F].pure(List.empty)
    } else {
      val keyGS = keyCodec.encode(key)
      val fieldsArray = fields.map(keyCodec.encode).toArray
      baseClient.hmget(keyGS, fieldsArray).futureLift
        .map { javaArray =>
          javaArray.toList.map(gs => Option(gs).map(valueCodec.decode))
        }
        .handleErrorWith { err =>
          Log[F].error(s"Error in HMGET $key: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def hdel(key: K, fields: K*): F[Long] = {
    if (fields.isEmpty) {
      Async[F].pure(0L)
    } else {
      val keyGS = keyCodec.encode(key)
      val fieldsArray = fields.map(keyCodec.encode).toArray
      baseClient.hdel(keyGS, fieldsArray).futureLift
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in HDEL $key: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def hexists(key: K, field: K): F[Boolean] = {
    val keyGS = keyCodec.encode(key)
    val fieldGS = keyCodec.encode(field)
    baseClient.hexists(keyGS, fieldGS).futureLift
      .map(_.booleanValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in HEXISTS $key $field: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hkeys(key: K): F[List[K]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.hkeys(keyGS).futureLift
      .map(_.toList.map(keyCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in HKEYS $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hvals(key: K): F[List[V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.hvals(keyGS).futureLift
      .map(_.toList.map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in HVALS $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hlen(key: K): F[Long] = {
    val keyGS = keyCodec.encode(key)
    baseClient.hlen(keyGS).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in HLEN $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hincrBy(key: K, field: K, increment: Long): F[Long] = {
    val keyGS = keyCodec.encode(key)
    val fieldGS = keyCodec.encode(field)
    baseClient.hincrBy(keyGS, fieldGS, increment).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(
          s"Error in HINCRBY $key $field $increment: ${err.getMessage}"
        ) *>
          Async[F].raiseError(err)
      }
  }

  override def hincrByFloat(key: K, field: K, increment: Double): F[Double] = {
    val keyGS = keyCodec.encode(key)
    val fieldGS = keyCodec.encode(field)
    baseClient.hincrByFloat(keyGS, fieldGS, increment).futureLift
      .map(_.doubleValue())
      .handleErrorWith { err =>
        Log[F].error(
          s"Error in HINCRBYFLOAT $key $field $increment: ${err.getMessage}"
        ) *>
          Async[F].raiseError(err)
      }
  }

  override def hsetnx(key: K, field: K, value: V): F[Boolean] = {
    val keyGS = keyCodec.encode(key)
    val fieldGS = keyCodec.encode(field)
    val valueGS = valueCodec.encode(value)
    baseClient.hsetnx(keyGS, fieldGS, valueGS).futureLift
      .map(_.booleanValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in HSETNX $key $field: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hstrlen(key: K, field: K): F[Long] = {
    val keyGS = keyCodec.encode(key)
    val fieldGS = keyCodec.encode(field)
    baseClient.hstrlen(keyGS, fieldGS).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in HSTRLEN $key $field: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hrandfield(key: K): F[Option[K]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.hrandfield(keyGS).futureLift
      .map(gs => Option(gs).map(keyCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in HRANDFIELD $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hrandfieldWithCount(key: K, count: Long): F[List[K]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.hrandfieldWithCount(keyGS, count).futureLift
      .map(_.toList.map(keyCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(
          s"Error in HRANDFIELDWITHCOUNT $key $count: ${err.getMessage}"
        ) *>
          Async[F].raiseError(err)
      }
  }

  override def hrandfieldWithCountWithValues(
      key: K,
      count: Long
  ): F[List[(K, V)]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.hrandfieldWithCountWithValues(keyGS, count).futureLift
      .map { javaArray =>
        javaArray.toList.map { pair =>
          keyCodec.decode(pair(0)) -> valueCodec.decode(pair(1))
        }
      }
      .handleErrorWith { err =>
        Log[F].error(
          s"Error in HRANDFIELDWITHCOUNTWITHVALUES $key $count: ${err.getMessage}"
        ) *>
          Async[F].raiseError(err)
      }
  }

  // ==================== List Commands ====================

  override def lpush(key: K, elements: V*): F[Long] = {
    if (elements.isEmpty) {
      Async[F].pure(0L)
    } else {
      val keyGS = keyCodec.encode(key)
      val elementsArray = elements.map(valueCodec.encode).toArray
      baseClient.lpush(keyGS, elementsArray).futureLift
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in LPUSH $key: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def rpush(key: K, elements: V*): F[Long] = {
    if (elements.isEmpty) {
      Async[F].pure(0L)
    } else {
      val keyGS = keyCodec.encode(key)
      val elementsArray = elements.map(valueCodec.encode).toArray
      baseClient.rpush(keyGS, elementsArray).futureLift
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in RPUSH $key: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def lpop(key: K): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.lpop(keyGS).futureLift
      .map(gs => Option(gs).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in LPOP $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def rpop(key: K): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.rpop(keyGS).futureLift
      .map(gs => Option(gs).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in RPOP $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def lpopCount(key: K, count: Long): F[List[V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.lpopCount(keyGS, count).futureLift
      .map(arr =>
        if (arr == null) List.empty else arr.toList.map(valueCodec.decode)
      )
      .handleErrorWith { err =>
        Log[F].error(s"Error in LPOPCOUNT $key $count: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def rpopCount(key: K, count: Long): F[List[V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.rpopCount(keyGS, count).futureLift
      .map(arr =>
        if (arr == null) List.empty else arr.toList.map(valueCodec.decode)
      )
      .handleErrorWith { err =>
        Log[F].error(s"Error in RPOPCOUNT $key $count: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def lrange(key: K, start: Long, stop: Long): F[List[V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.lrange(keyGS, start, stop).futureLift
      .map(_.toList.map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in LRANGE $key $start $stop: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def lindex(key: K, index: Long): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.lindex(keyGS, index).futureLift
      .map(gs => Option(gs).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in LINDEX $key $index: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def llen(key: K): F[Long] = {
    val keyGS = keyCodec.encode(key)
    baseClient.llen(keyGS).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in LLEN $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def ltrim(key: K, start: Long, stop: Long): F[Unit] = {
    val keyGS = keyCodec.encode(key)
    baseClient.ltrim(keyGS, start, stop).futureLift
      .void
      .handleErrorWith { err =>
        Log[F].error(s"Error in LTRIM $key $start $stop: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def lset(key: K, index: Long, element: V): F[Unit] = {
    val keyGS = keyCodec.encode(key)
    val elementGS = valueCodec.encode(element)
    baseClient.lset(keyGS, index, elementGS).futureLift
      .void
      .handleErrorWith { err =>
        Log[F].error(s"Error in LSET $key $index: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def lrem(key: K, count: Long, element: V): F[Long] = {
    val keyGS = keyCodec.encode(key)
    val elementGS = valueCodec.encode(element)
    baseClient.lrem(keyGS, count, elementGS).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in LREM $key $count: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def linsert(
      key: K,
      before: Boolean,
      pivot: V,
      element: V
  ): F[Long] = {
    val keyGS = keyCodec.encode(key)
    val pivotGS = valueCodec.encode(pivot)
    val elementGS = valueCodec.encode(element)
    val position = if (before) {
      glide.api.models.commands.LInsertOptions.InsertPosition.BEFORE
    } else {
      glide.api.models.commands.LInsertOptions.InsertPosition.AFTER
    }
    baseClient.linsert(keyGS, position, pivotGS, elementGS).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in LINSERT $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def linsert(
      key: K,
      position: InsertPosition,
      pivot: V,
      element: V
  ): F[Long] = {
    val keyGS = keyCodec.encode(key)
    val pivotGS = valueCodec.encode(pivot)
    val elementGS = valueCodec.encode(element)
    val glidePosition = position.toGlide

    baseClient.linsert(keyGS, glidePosition, pivotGS, elementGS).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in LINSERT $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def lpos(key: K, element: V): F[Option[Long]] = {
    val keyGS = keyCodec.encode(key)
    val elementGS = valueCodec.encode(element)
    baseClient.lpos(keyGS, elementGS).futureLift
      .map(result => Option(result).map(_.longValue()))
      .handleErrorWith { err =>
        Log[F].error(s"Error in LPOS $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  // ==================== Set Commands ====================

  override def sadd(key: K, members: V*): F[Long] = {
    if (members.isEmpty) {
      Async[F].pure(0L)
    } else {
      val keyGS = keyCodec.encode(key)
      val membersArray = members.map(valueCodec.encode).toArray
      baseClient.sadd(keyGS, membersArray).futureLift
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in SADD $key: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def srem(key: K, members: V*): F[Long] = {
    if (members.isEmpty) {
      Async[F].pure(0L)
    } else {
      val keyGS = keyCodec.encode(key)
      val membersArray = members.map(valueCodec.encode).toArray
      baseClient.srem(keyGS, membersArray).futureLift
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in SREM $key: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def smembers(key: K): F[Set[V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.smembers(keyGS).futureLift
      .map { javaSet =>
        import scala.jdk.CollectionConverters.*
        javaSet.asScala.map(valueCodec.decode).toSet
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in SMEMBERS $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def sismember(key: K, member: V): F[Boolean] = {
    val keyGS = keyCodec.encode(key)
    val memberGS = valueCodec.encode(member)
    baseClient.sismember(keyGS, memberGS).futureLift
      .map(_.booleanValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in SISMEMBER $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def smismember(key: K, members: V*): F[List[Boolean]] = {
    if (members.isEmpty) {
      Async[F].pure(List.empty[Boolean])
    } else {
      val keyGS = keyCodec.encode(key)
      val membersArray = members.map(valueCodec.encode).toArray
      baseClient.smismember(keyGS, membersArray).futureLift
        .map { javaArray =>
          javaArray.map(_.booleanValue()).toList
        }
        .handleErrorWith { err =>
          Log[F].error(s"Error in SMISMEMBER $key: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def scard(key: K): F[Long] = {
    val keyGS = keyCodec.encode(key)
    baseClient.scard(keyGS).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in SCARD $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def sunion(keys: K*): F[Set[V]] = {
    if (keys.isEmpty) {
      Async[F].pure(Set.empty[V])
    } else {
      val keysArray = keys.map(keyCodec.encode).toArray
      baseClient.sunion(keysArray).futureLift
        .map { javaSet =>
          import scala.jdk.CollectionConverters.*
          javaSet.asScala.map(valueCodec.decode).toSet
        }
        .handleErrorWith { err =>
          Log[F].error(s"Error in SUNION: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def sunionstore(destination: K, keys: K*): F[Long] = {
    if (keys.isEmpty) {
      Async[F].pure(0L)
    } else {
      val destGS = keyCodec.encode(destination)
      val keysArray = keys.map(keyCodec.encode).toArray
      baseClient.sunionstore(destGS, keysArray).futureLift
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(
            s"Error in SUNIONSTORE $destination: ${err.getMessage}"
          ) *>
            Async[F].raiseError(err)
        }
    }
  }

  override def sinter(keys: K*): F[Set[V]] = {
    if (keys.isEmpty) {
      Async[F].pure(Set.empty[V])
    } else {
      val keysArray = keys.map(keyCodec.encode).toArray
      baseClient.sinter(keysArray).futureLift
        .map { javaSet =>
          import scala.jdk.CollectionConverters.*
          javaSet.asScala.map(valueCodec.decode).toSet
        }
        .handleErrorWith { err =>
          Log[F].error(s"Error in SINTER: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def sinterstore(destination: K, keys: K*): F[Long] = {
    if (keys.isEmpty) {
      Async[F].pure(0L)
    } else {
      val destGS = keyCodec.encode(destination)
      val keysArray = keys.map(keyCodec.encode).toArray
      baseClient.sinterstore(destGS, keysArray).futureLift
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(
            s"Error in SINTERSTORE $destination: ${err.getMessage}"
          ) *>
            Async[F].raiseError(err)
        }
    }
  }

  override def sdiff(keys: K*): F[Set[V]] = {
    if (keys.isEmpty) {
      Async[F].pure(Set.empty[V])
    } else {
      val keysArray = keys.map(keyCodec.encode).toArray
      baseClient.sdiff(keysArray).futureLift
        .map { javaSet =>
          import scala.jdk.CollectionConverters.*
          javaSet.asScala.map(valueCodec.decode).toSet
        }
        .handleErrorWith { err =>
          Log[F].error(s"Error in SDIFF: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def sdiffstore(destination: K, keys: K*): F[Long] = {
    if (keys.isEmpty) {
      Async[F].pure(0L)
    } else {
      val destGS = keyCodec.encode(destination)
      val keysArray = keys.map(keyCodec.encode).toArray
      baseClient.sdiffstore(destGS, keysArray).futureLift
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(
            s"Error in SDIFFSTORE $destination: ${err.getMessage}"
          ) *>
            Async[F].raiseError(err)
        }
    }
  }

  override def spop(key: K): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.spop(keyGS).futureLift
      .map(result => Option(result).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in SPOP $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def spopCount(key: K, count: Long): F[Set[V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.spopCount(keyGS, count).futureLift
      .map { javaSet =>
        import scala.jdk.CollectionConverters.*
        javaSet.asScala.map(valueCodec.decode).toSet
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in SPOP $key $count: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def srandmember(key: K): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.srandmember(keyGS).futureLift
      .map(result => Option(result).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in SRANDMEMBER $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def srandmemberCount(key: K, count: Long): F[List[V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.srandmember(keyGS, count).futureLift
      .map { javaArray =>
        javaArray.toList.map(valueCodec.decode)
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in SRANDMEMBER $key $count: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def smove(source: K, destination: K, member: V): F[Boolean] = {
    val sourceGS = keyCodec.encode(source)
    val destGS = keyCodec.encode(destination)
    val memberGS = valueCodec.encode(member)
    baseClient.smove(sourceGS, destGS, memberGS).futureLift
      .map(_.booleanValue())
      .handleErrorWith { err =>
        Log[F].error(
          s"Error in SMOVE $source $destination: ${err.getMessage}"
        ) *>
          Async[F].raiseError(err)
      }
  }

  // ==================== Sorted Set Commands ====================

  override def zadd(key: K, membersScores: Map[V, Double]): F[Long] = {
    if (membersScores.isEmpty) {
      Async[F].pure(0L)
    } else {
      val keyGS = keyCodec.encode(key)
      val javaMap = membersScores.map { case (member, score) =>
        valueCodec.encode(member) -> Double.box(score)
      }.asJava

      baseClient.zadd(keyGS, javaMap).futureLift
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in ZADD $key: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def zadd(
      key: K,
      membersScores: Map[V, Double],
      options: ZAddOptions
  ): F[Long] = {
    if (membersScores.isEmpty) {
      Async[F].pure(0L)
    } else {
      val keyGS = keyCodec.encode(key)
      val javaMap = membersScores.map { case (member, score) =>
        valueCodec.encode(member) -> Double.box(score)
      }.asJava
      val glideOptions = ZAddOptions.toGlide(options)

      baseClient.zadd(keyGS, javaMap, glideOptions).futureLift
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(
            s"Error in ZADD $key (with options): ${err.getMessage}"
          ) *>
            Async[F].raiseError(err)
        }
    }
  }

  override def zrem(key: K, members: V*): F[Long] = {
    if (members.isEmpty) {
      Async[F].pure(0L)
    } else {
      val keyGS = keyCodec.encode(key)
      val membersArray = members.map(valueCodec.encode).toArray
      baseClient.zrem(keyGS, membersArray).futureLift
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in ZREM $key: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def zrange(key: K, start: Long, stop: Long): F[List[V]] = {
    val keyGS = keyCodec.encode(key)
    val rangeQuery =
      new glide.api.models.commands.RangeOptions.RangeByIndex(start, stop)
    baseClient.zrange(keyGS, rangeQuery).futureLift
      .map { javaArray =>
        javaArray.toList.map(valueCodec.decode)
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZRANGE $key $start $stop: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zrangeWithScores(
      key: K,
      start: Long,
      stop: Long
  ): F[List[(V, Double)]] = {
    val keyGS = keyCodec.encode(key)
    val rangeQuery =
      new glide.api.models.commands.RangeOptions.RangeByIndex(start, stop)
    baseClient.zrangeWithScores(keyGS, rangeQuery).futureLift
      .map { javaMap =>
        import scala.jdk.CollectionConverters.*
        javaMap.asScala.toList.map { case (gs, score) =>
          (valueCodec.decode(gs), score.doubleValue())
        }
      }
      .handleErrorWith { err =>
        Log[F].error(
          s"Error in ZRANGE (with scores) $key $start $stop: ${err.getMessage}"
        ) *>
          Async[F].raiseError(err)
      }
  }

  override def zscore(key: K, member: V): F[Option[Double]] = {
    val keyGS = keyCodec.encode(key)
    val memberGS = valueCodec.encode(member)
    baseClient.zscore(keyGS, memberGS).futureLift
      .map(result => Option(result).map(_.doubleValue()))
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZSCORE $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zmscore(key: K, members: V*): F[List[Option[Double]]] = {
    if (members.isEmpty) {
      Async[F].pure(List.empty[Option[Double]])
    } else {
      val keyGS = keyCodec.encode(key)
      val membersArray = members.map(valueCodec.encode).toArray
      baseClient.zmscore(keyGS, membersArray).futureLift
        .map { javaArray =>
          javaArray.toList.map(score => Option(score).map(_.doubleValue()))
        }
        .handleErrorWith { err =>
          Log[F].error(s"Error in ZMSCORE $key: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def zcard(key: K): F[Long] = {
    val keyGS = keyCodec.encode(key)
    baseClient.zcard(keyGS).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZCARD $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zrank(key: K, member: V): F[Option[Long]] = {
    val keyGS = keyCodec.encode(key)
    val memberGS = valueCodec.encode(member)
    baseClient.zrank(keyGS, memberGS).futureLift
      .map(result => Option(result).map(_.longValue()))
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZRANK $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zrevrank(key: K, member: V): F[Option[Long]] = {
    val keyGS = keyCodec.encode(key)
    val memberGS = valueCodec.encode(member)
    baseClient.zrevrank(keyGS, memberGS).futureLift
      .map(result => Option(result).map(_.longValue()))
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZREVRANK $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zincrby(key: K, increment: Double, member: V): F[Double] = {
    val keyGS = keyCodec.encode(key)
    val memberGS = valueCodec.encode(member)
    baseClient.zincrby(keyGS, increment, memberGS).futureLift
      .map(_.doubleValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZINCRBY $key $increment: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zcount(key: K, min: Double, max: Double): F[Long] = {
    val keyGS = keyCodec.encode(key)
    val minScore =
      new glide.api.models.commands.RangeOptions.ScoreBoundary(min, true)
    val maxScore =
      new glide.api.models.commands.RangeOptions.ScoreBoundary(max, true)
    baseClient.zcount(keyGS, minScore, maxScore).futureLift
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZCOUNT $key $min $max: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zpopmin(key: K): F[Option[(V, Double)]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.zpopmin(keyGS).futureLift
      .map { javaMap =>
        import scala.jdk.CollectionConverters.*
        javaMap.asScala.headOption.map { case (gs, score) =>
          (valueCodec.decode(gs), score.doubleValue())
        }
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZPOPMIN $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zpopminCount(key: K, count: Long): F[List[(V, Double)]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.zpopmin(keyGS, count).futureLift
      .map { javaMap =>
        import scala.jdk.CollectionConverters.*
        javaMap.asScala.toList.map { case (gs, score) =>
          (valueCodec.decode(gs), score.doubleValue())
        }
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZPOPMIN $key $count: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zpopmax(key: K): F[Option[(V, Double)]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.zpopmax(keyGS).futureLift
      .map { javaMap =>
        import scala.jdk.CollectionConverters.*
        javaMap.asScala.headOption.map { case (gs, score) =>
          (valueCodec.decode(gs), score.doubleValue())
        }
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZPOPMAX $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zpopmaxCount(key: K, count: Long): F[List[(V, Double)]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.zpopmax(keyGS, count).futureLift
      .map { javaMap =>
        import scala.jdk.CollectionConverters.*
        javaMap.asScala.toList.map { case (gs, score) =>
          (valueCodec.decode(gs), score.doubleValue())
        }
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZPOPMAX $key $count: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zrandmember(key: K): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.zrandmember(keyGS).futureLift
      .map(result => Option(result).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZRANDMEMBER $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zrandmemberCount(key: K, count: Long): F[List[V]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.zrandmemberWithCount(keyGS, count).futureLift
      .map { javaArray =>
        javaArray.toList.map(valueCodec.decode)
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZRANDMEMBER $key $count: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zrandmemberWithScores(
      key: K,
      count: Long
  ): F[List[(V, Double)]] = {
    val keyGS = keyCodec.encode(key)
    baseClient.zrandmemberWithCountWithScores(keyGS, count).futureLift
      .map { javaArray =>
        javaArray.toList.map { pair =>
          val gs = pair(0).asInstanceOf[glide.api.models.GlideString]
          val score = pair(1).asInstanceOf[java.lang.Double]
          (valueCodec.decode(gs), score.doubleValue())
        }
      }
      .handleErrorWith { err =>
        Log[F].error(
          s"Error in ZRANDMEMBER (with scores) $key $count: ${err.getMessage}"
        ) *>
          Async[F].raiseError(err)
      }
  }
}

/** Standalone client commands implementation */
private[valkey4cats] class ValkeyStandalone[F[_]: MkValkey: Async, K, V](
    client: ValkeyClient,
    keyCodec: Codec[K],
    valueCodec: Codec[V],
    tx: TxRunner[F]
) extends BaseValkey[F, K, V](Left(client), keyCodec, valueCodec, tx)

/** Cluster client commands implementation */
private[valkey4cats] class ValkeyCluster[F[_]: MkValkey: Async, K, V](
    client: ValkeyClusterClient,
    keyCodec: Codec[K],
    valueCodec: Codec[V],
    tx: TxRunner[F]
) extends BaseValkey[F, K, V](Right(client), keyCodec, valueCodec, tx)
