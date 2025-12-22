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
