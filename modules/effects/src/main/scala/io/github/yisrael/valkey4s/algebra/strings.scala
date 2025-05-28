

package io.github.yisrael.valkey4s.algebra

import scala.concurrent.duration.FiniteDuration

import io.github.yisrael.valkey4s.effects.{ GetExArg, SetArgs }

import io.lettuce.core.RedisFuture
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands

trait StringCommands[F[_], K, V]
    extends Getter[F, K, V]
    with Setter[F, K, V]
    with MultiKey[F, K, V]
    with Decrement[F, K, V]
    with Increment[F, K, V]
    with Unsafe[F, K, V]

trait Getter[F[_], K, V] {
  def get(key: K): F[Option[V]]
  def getEx(key: K, getExArg: GetExArg): F[Option[V]]
  def getRange(key: K, start: Long, end: Long): F[Option[V]]
  def strLen(key: K): F[Long]
}

trait Setter[F[_], K, V] {
  def append(key: K, value: V): F[Unit]
  def getSet(key: K, value: V): F[Option[V]]
  def set(key: K, value: V): F[Unit]
  def set(key: K, value: V, setArgs: SetArgs): F[Boolean]
  def setNx(key: K, value: V): F[Boolean]
  def setEx(key: K, value: V, expiresIn: FiniteDuration): F[Unit]
  def setRange(key: K, value: V, offset: Long): F[Unit]
}

trait MultiKey[F[_], K, V] {
  def mGet(keys: Set[K]): F[Map[K, V]]
  def mSet(keyValues: Map[K, V]): F[Unit]
  def mSetNx(keyValues: Map[K, V]): F[Boolean]
}

trait Decrement[F[_], K, V] {
  def decr(key: K): F[Long]
  def decrBy(key: K, amount: Long): F[Long]
}

trait Increment[F[_], K, V] {
  def incr(key: K): F[Long]
  def incrBy(key: K, amount: Long): F[Long]
  def incrByFloat(key: K, amount: Double): F[Double]
}

trait Unsafe[F[_], K, V] {

  /** USE WITH CAUTION! It gives you access to the underlying Java API.
    *
    * Useful whenever Redis4cats does not yet support the operation you're looking for.
    */
  def unsafe[A](f: RedisClusterAsyncCommands[K, V] => RedisFuture[A]): F[A]

  /** USE WITH CAUTION! It gives you access to the underlying Java API.
    *
    * Useful whenever Redis4cats does not yet support the operation you're looking for.
    */
  def unsafeSync[A](f: RedisClusterAsyncCommands[K, V] => A): F[A]
}
