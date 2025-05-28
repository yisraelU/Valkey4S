

package io.github.yisrael.valkey4s.algebra

trait HyperLogLogCommands[F[_], K, V] {
  def pfAdd(key: K, values: V*): F[Long]
  def pfCount(key: K): F[Long]
  def pfMerge(outputKey: K, inputKeys: K*): F[Unit]
}
