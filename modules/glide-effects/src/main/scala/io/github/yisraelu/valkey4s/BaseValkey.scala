package io.github.yisraelu.valkey4s

import cats.effect._
import cats.syntax.all._
import glide.api.BaseClient
import io.github.yisraelu.valkey4s.codec.Codec
import io.github.yisraelu.valkey4s.connection.{
  ValkeyClient,
  ValkeyClusterClient
}
import io.github.yisraelu.valkey4s.effect.{Log, TxRunner}

import scala.jdk.CollectionConverters._

/** Base implementation for Valkey commands
  *
  * Supports both standalone and cluster clients through the BaseClient abstraction
  *
  * @param client Either standalone or cluster client
  * @param keyCodec Codec for encoding/decoding keys
  * @param valueCodec Codec for encoding/decoding values
  * @param tx Transaction runner (stub for Phase 1)
  */
private[valkey4s] abstract class BaseValkey[F[_]: Async: Log, K, V](
                                                                     protected val client: Either[ValkeyClient[F], ValkeyClusterClient[F]],
                                                                     protected val keyCodec: Codec[K],
                                                                     protected val valueCodec: Codec[V],
                                                                     protected val tx: TxRunner[F]
) extends ValkeyCommands[F, K, V] {

  /** Get the underlying Glide BaseClient (works for both standalone and cluster) */
  private val baseClient: BaseClient = client match {
    case Left(standalone) => standalone.underlying
    case Right(cluster)   => cluster.underlying
  }

  // ==================== String Commands ====================

  override def get(key: K): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.get(keyGS))
      )
      .map(gs => Option(gs).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in GET $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def set(key: K, value: V): F[Unit] = {
    val keyGS = keyCodec.encode(key)
    val valueGS = valueCodec.encode(value)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.set(keyGS, valueGS))
      )
      .void
      .handleErrorWith { err =>
        Log[F].error(s"Error in SET $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def set(key: K, value: V, options: io.github.yisraelu.valkey4s.model.SetOptions): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    val valueGS = valueCodec.encode(value)
    val glideOptions = options.toGlide

    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.set(keyGS, valueGS, glideOptions))
      )
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.mget(keysArray))
        )
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

      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.mset(javaMap))
        )
        .void
        .handleErrorWith { err =>
          Log[F].error(s"Error in MSET: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def incr(key: K): F[Long] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.incr(keyGS))
      )
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in INCR $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def incrBy(key: K, amount: Long): F[Long] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.incrBy(keyGS, amount))
      )
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in INCRBY $key $amount: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def decr(key: K): F[Long] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.decr(keyGS))
      )
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in DECR $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def decrBy(key: K, amount: Long): F[Long] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.decrBy(keyGS, amount))
      )
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in DECRBY $key $amount: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def append(key: K, value: V): F[Long] = {
    val keyGS = keyCodec.encode(key)
    val valueGS = valueCodec.encode(value)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.append(keyGS, valueGS))
      )
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in APPEND $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def strlen(key: K): F[Long] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.strlen(keyGS))
      )
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.del(keysArray))
        )
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in DEL: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def exists(key: K): F[Boolean] = {
    val keysArray = Array(keyCodec.encode(key))
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.exists(keysArray))
      )
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.exists(keysArray))
        )
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

      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.hset(keyGS, javaMap))
        )
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
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.hget(keyGS, fieldGS))
      )
      .map(gs => Option(gs).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in HGET $key $field: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hgetall(key: K): F[Map[K, V]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.hgetall(keyGS))
      )
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.hmget(keyGS, fieldsArray))
        )
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.hdel(keyGS, fieldsArray))
        )
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
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.hexists(keyGS, fieldGS))
      )
      .map(_.booleanValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in HEXISTS $key $field: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hkeys(key: K): F[List[K]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.hkeys(keyGS))
      )
      .map(_.toList.map(keyCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in HKEYS $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hvals(key: K): F[List[V]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.hvals(keyGS))
      )
      .map(_.toList.map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in HVALS $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hlen(key: K): F[Long] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.hlen(keyGS))
      )
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in HLEN $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hincrBy(key: K, field: K, increment: Long): F[Long] = {
    val keyGS = keyCodec.encode(key)
    val fieldGS = keyCodec.encode(field)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.hincrBy(keyGS, fieldGS, increment))
      )
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in HINCRBY $key $field $increment: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hincrByFloat(key: K, field: K, increment: Double): F[Double] = {
    val keyGS = keyCodec.encode(key)
    val fieldGS = keyCodec.encode(field)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.hincrByFloat(keyGS, fieldGS, increment))
      )
      .map(_.doubleValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in HINCRBYFLOAT $key $field $increment: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hsetnx(key: K, field: K, value: V): F[Boolean] = {
    val keyGS = keyCodec.encode(key)
    val fieldGS = keyCodec.encode(field)
    val valueGS = valueCodec.encode(value)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.hsetnx(keyGS, fieldGS, valueGS))
      )
      .map(_.booleanValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in HSETNX $key $field: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hstrlen(key: K, field: K): F[Long] = {
    val keyGS = keyCodec.encode(key)
    val fieldGS = keyCodec.encode(field)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.hstrlen(keyGS, fieldGS))
      )
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in HSTRLEN $key $field: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hrandfield(key: K): F[Option[K]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.hrandfield(keyGS))
      )
      .map(gs => Option(gs).map(keyCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in HRANDFIELD $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hrandfieldWithCount(key: K, count: Long): F[List[K]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.hrandfieldWithCount(keyGS, count))
      )
      .map(_.toList.map(keyCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in HRANDFIELDWITHCOUNT $key $count: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def hrandfieldWithCountWithValues(key: K, count: Long): F[List[(K, V)]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.hrandfieldWithCountWithValues(keyGS, count))
      )
      .map { javaArray =>
        javaArray.toList.map { pair =>
          keyCodec.decode(pair(0)) -> valueCodec.decode(pair(1))
        }
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in HRANDFIELDWITHCOUNTWITHVALUES $key $count: ${err.getMessage}") *>
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.lpush(keyGS, elementsArray))
        )
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.rpush(keyGS, elementsArray))
        )
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in RPUSH $key: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def lpop(key: K): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.lpop(keyGS))
      )
      .map(gs => Option(gs).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in LPOP $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def rpop(key: K): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.rpop(keyGS))
      )
      .map(gs => Option(gs).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in RPOP $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def lpopCount(key: K, count: Long): F[List[V]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.lpopCount(keyGS, count))
      )
      .map(arr => if (arr == null) List.empty else arr.toList.map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in LPOPCOUNT $key $count: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def rpopCount(key: K, count: Long): F[List[V]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.rpopCount(keyGS, count))
      )
      .map(arr => if (arr == null) List.empty else arr.toList.map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in RPOPCOUNT $key $count: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def lrange(key: K, start: Long, stop: Long): F[List[V]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.lrange(keyGS, start, stop))
      )
      .map(_.toList.map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in LRANGE $key $start $stop: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def lindex(key: K, index: Long): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.lindex(keyGS, index))
      )
      .map(gs => Option(gs).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in LINDEX $key $index: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def llen(key: K): F[Long] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.llen(keyGS))
      )
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in LLEN $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def ltrim(key: K, start: Long, stop: Long): F[Unit] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.ltrim(keyGS, start, stop))
      )
      .void
      .handleErrorWith { err =>
        Log[F].error(s"Error in LTRIM $key $start $stop: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def lset(key: K, index: Long, element: V): F[Unit] = {
    val keyGS = keyCodec.encode(key)
    val elementGS = valueCodec.encode(element)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.lset(keyGS, index, elementGS))
      )
      .void
      .handleErrorWith { err =>
        Log[F].error(s"Error in LSET $key $index: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def lrem(key: K, count: Long, element: V): F[Long] = {
    val keyGS = keyCodec.encode(key)
    val elementGS = valueCodec.encode(element)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.lrem(keyGS, count, elementGS))
      )
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in LREM $key $count: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def linsert(key: K, before: Boolean, pivot: V, element: V): F[Long] = {
    val keyGS = keyCodec.encode(key)
    val pivotGS = valueCodec.encode(pivot)
    val elementGS = valueCodec.encode(element)
    val position = if (before) {
      glide.api.models.commands.LInsertOptions.InsertPosition.BEFORE
    } else {
      glide.api.models.commands.LInsertOptions.InsertPosition.AFTER
    }
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.linsert(keyGS, position, pivotGS, elementGS))
      )
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in LINSERT $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def lpos(key: K, element: V): F[Option[Long]] = {
    val keyGS = keyCodec.encode(key)
    val elementGS = valueCodec.encode(element)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.lpos(keyGS, elementGS))
      )
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.sadd(keyGS, membersArray))
        )
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.srem(keyGS, membersArray))
        )
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in SREM $key: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def smembers(key: K): F[Set[V]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.smembers(keyGS))
      )
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
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.sismember(keyGS, memberGS))
      )
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.smismember(keyGS, membersArray))
        )
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
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.scard(keyGS))
      )
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.sunion(keysArray))
        )
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.sunionstore(destGS, keysArray))
        )
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in SUNIONSTORE $destination: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def sinter(keys: K*): F[Set[V]] = {
    if (keys.isEmpty) {
      Async[F].pure(Set.empty[V])
    } else {
      val keysArray = keys.map(keyCodec.encode).toArray
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.sinter(keysArray))
        )
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.sinterstore(destGS, keysArray))
        )
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in SINTERSTORE $destination: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def sdiff(keys: K*): F[Set[V]] = {
    if (keys.isEmpty) {
      Async[F].pure(Set.empty[V])
    } else {
      val keysArray = keys.map(keyCodec.encode).toArray
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.sdiff(keysArray))
        )
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.sdiffstore(destGS, keysArray))
        )
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in SDIFFSTORE $destination: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def spop(key: K): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.spop(keyGS))
      )
      .map(result => Option(result).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in SPOP $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def spopCount(key: K, count: Long): F[Set[V]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.spopCount(keyGS, count))
      )
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
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.srandmember(keyGS))
      )
      .map(result => Option(result).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in SRANDMEMBER $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def srandmemberCount(key: K, count: Long): F[List[V]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.srandmember(keyGS, count))
      )
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
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.smove(sourceGS, destGS, memberGS))
      )
      .map(_.booleanValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in SMOVE $source $destination: ${err.getMessage}") *>
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

      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.zadd(keyGS, javaMap))
        )
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in ZADD $key: ${err.getMessage}") *>
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.zrem(keyGS, membersArray))
        )
        .map(_.longValue())
        .handleErrorWith { err =>
          Log[F].error(s"Error in ZREM $key: ${err.getMessage}") *>
            Async[F].raiseError(err)
        }
    }
  }

  override def zrange(key: K, start: Long, stop: Long): F[List[V]] = {
    val keyGS = keyCodec.encode(key)
    val rangeQuery = new glide.api.models.commands.RangeOptions.RangeByIndex(start, stop)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.zrange(keyGS, rangeQuery))
      )
      .map { javaArray =>
        javaArray.toList.map(valueCodec.decode)
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZRANGE $key $start $stop: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zrangeWithScores(key: K, start: Long, stop: Long): F[List[(V, Double)]] = {
    val keyGS = keyCodec.encode(key)
    val rangeQuery = new glide.api.models.commands.RangeOptions.RangeByIndex(start, stop)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.zrangeWithScores(keyGS, rangeQuery))
      )
      .map { javaMap =>
        import scala.jdk.CollectionConverters.*
        javaMap.asScala.toList.map { case (gs, score) =>
          (valueCodec.decode(gs), score.doubleValue())
        }
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZRANGE (with scores) $key $start $stop: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zscore(key: K, member: V): F[Option[Double]] = {
    val keyGS = keyCodec.encode(key)
    val memberGS = valueCodec.encode(member)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.zscore(keyGS, memberGS))
      )
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
      Async[F]
        .fromCompletableFuture(
          Async[F].delay(baseClient.zmscore(keyGS, membersArray))
        )
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
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.zcard(keyGS))
      )
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZCARD $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zrank(key: K, member: V): F[Option[Long]] = {
    val keyGS = keyCodec.encode(key)
    val memberGS = valueCodec.encode(member)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.zrank(keyGS, memberGS))
      )
      .map(result => Option(result).map(_.longValue()))
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZRANK $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zrevrank(key: K, member: V): F[Option[Long]] = {
    val keyGS = keyCodec.encode(key)
    val memberGS = valueCodec.encode(member)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.zrevrank(keyGS, memberGS))
      )
      .map(result => Option(result).map(_.longValue()))
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZREVRANK $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zincrby(key: K, increment: Double, member: V): F[Double] = {
    val keyGS = keyCodec.encode(key)
    val memberGS = valueCodec.encode(member)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.zincrby(keyGS, increment, memberGS))
      )
      .map(_.doubleValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZINCRBY $key $increment: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zcount(key: K, min: Double, max: Double): F[Long] = {
    val keyGS = keyCodec.encode(key)
    val minScore = new glide.api.models.commands.RangeOptions.ScoreBoundary(min, true)
    val maxScore = new glide.api.models.commands.RangeOptions.ScoreBoundary(max, true)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.zcount(keyGS, minScore, maxScore))
      )
      .map(_.longValue())
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZCOUNT $key $min $max: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zpopmin(key: K): F[Option[(V, Double)]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.zpopmin(keyGS))
      )
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
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.zpopmin(keyGS, count))
      )
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
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.zpopmax(keyGS))
      )
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
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.zpopmax(keyGS, count))
      )
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
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.zrandmember(keyGS))
      )
      .map(result => Option(result).map(valueCodec.decode))
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZRANDMEMBER $key: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zrandmemberCount(key: K, count: Long): F[List[V]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.zrandmemberWithCount(keyGS, count))
      )
      .map { javaArray =>
        javaArray.toList.map(valueCodec.decode)
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZRANDMEMBER $key $count: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }

  override def zrandmemberWithScores(key: K, count: Long): F[List[(V, Double)]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(baseClient.zrandmemberWithCountWithScores(keyGS, count))
      )
      .map { javaArray =>
        javaArray.toList.map { pair =>
          val gs = pair(0).asInstanceOf[glide.api.models.GlideString]
          val score = pair(1).asInstanceOf[java.lang.Double]
          (valueCodec.decode(gs), score.doubleValue())
        }
      }
      .handleErrorWith { err =>
        Log[F].error(s"Error in ZRANDMEMBER (with scores) $key $count: ${err.getMessage}") *>
          Async[F].raiseError(err)
      }
  }
}

/** Standalone client commands implementation */
private[valkey4s] class ValkeyStandalone[F[_]: Async: Log, K, V](
                                                                  client: ValkeyClient[F],
                                                                  keyCodec: Codec[K],
                                                                  valueCodec: Codec[V],
                                                                  tx: TxRunner[F]
) extends BaseValkey[F, K, V](Left(client), keyCodec, valueCodec, tx)

/** Cluster client commands implementation */
private[valkey4s] class ValkeyCluster[F[_]: Async: Log, K, V](
                                                               client: ValkeyClusterClient[F],
                                                               keyCodec: Codec[K],
                                                               valueCodec: Codec[V],
                                                               tx: TxRunner[F]
) extends BaseValkey[F, K, V](Right(client), keyCodec, valueCodec, tx)
