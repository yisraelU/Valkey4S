

package io.github.yisrael.valkey4s.algebra

import cats.data.NonEmptyList

import scala.concurrent.duration.Duration

trait ListCommands[F[_], K, V]
    extends ListBlocking[F, K, V]
    with ListGetter[F, K, V]
    with ListSetter[F, K, V]
    with ListPushPop[F, K, V]

trait ListBlocking[F[_], K, V] {
  def blPop(timeout: Duration, keys: NonEmptyList[K]): F[Option[(K, V)]]
  def brPop(timeout: Duration, keys: NonEmptyList[K]): F[Option[(K, V)]]
  def brPopLPush(timeout: Duration, source: K, destination: K): F[Option[V]]
}

trait ListGetter[F[_], K, V] {
  def lIndex(key: K, index: Long): F[Option[V]]
  def lLen(key: K): F[Long]
  def lRange(key: K, start: Long, stop: Long): F[List[V]]
}

trait ListSetter[F[_], K, V] {
  def lInsertAfter(key: K, pivot: V, value: V): F[Long]
  def lInsertBefore(key: K, pivot: V, value: V): F[Long]
  def lRem(key: K, count: Long, value: V): F[Long]
  def lSet(key: K, index: Long, value: V): F[Unit]
  def lTrim(key: K, start: Long, stop: Long): F[Unit]
}

trait ListPushPop[F[_], K, V] {
  def lPop(key: K): F[Option[V]]
  def lPush(key: K, values: V*): F[Long]
  def lPushX(key: K, values: V*): F[Long]
  def rPop(key: K): F[Option[V]]
  def rPopLPush(source: K, destination: K): F[Option[V]]
  def rPush(key: K, values: V*): F[Long]
  def rPushX(key: K, values: V*): F[Long]
}
