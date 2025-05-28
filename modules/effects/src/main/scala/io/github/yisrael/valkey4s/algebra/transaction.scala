

package io.github.yisrael.valkey4s.algebra

import dev.profunktor.redis4cats.tx.TxStore

trait TransactionalCommands[F[_], K] extends Transaction[F] with Watcher[F, K] with HighLevelTx[F] with Pipelining[F]

trait Transaction[F[_]] {
  def multi: F[Unit]
  def exec: F[Unit]
  def discard: F[Unit]
}

trait Watcher[F[_], K] {
  def watch(keys: K*): F[Unit]
  def unwatch: F[Unit]
}

trait HighLevelTx[F[_]] {
  def transact[A](fs: TxStore[F, String, A] => List[F[Unit]]): F[Map[String, A]]
  def transact_(fs: List[F[Unit]]): F[Unit]
}

trait Pipelining[F[_]] {
  def pipeline[A](fs: TxStore[F, String, A] => List[F[Unit]]): F[Map[String, A]]
  def pipeline_(fs: List[F[Unit]]): F[Unit]
}
