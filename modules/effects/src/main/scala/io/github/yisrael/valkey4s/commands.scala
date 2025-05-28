

package io.github.yisrael.valkey4s

import cats.effect.kernel.Async
import io.github.yisrael.valkey4s.algebra.*

trait RedisCommands[F[_], K, V]
    extends StringCommands[F, K, V]
    with HashCommands[F, K, V]
    with SetCommands[F, K, V]
    with SortedSetCommands[F, K, V]
    with ListCommands[F, K, V]
    with GeoCommands[F, K, V]
    with ConnectionCommands[F, K]
    with ServerCommands[F, K]
    with TransactionalCommands[F, K]
    with PipelineCommands[F]
    with ScriptCommands[F, K, V]
    with KeyCommands[F, K]
    with HyperLogLogCommands[F, K, V]
    with BitCommands[F, K, V]
    with StreamCommands[F, K, V]

object RedisCommands {
  implicit class LiftKOps[F[_], K, V](val cmd: RedisCommands[F, K, V]) extends AnyVal {
    def liftK[G[_]: Async: Log]: RedisCommands[G, K, V] =
      cmd.asInstanceOf[BaseRedis[F, K, V]].liftK[G]
  }
}
