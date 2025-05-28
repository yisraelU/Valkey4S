

package io.github.yisrael.valkey4s.algebra

import io.github.yisrael.valkey4s.effects.FlushMode

import java.time.Instant

trait ServerCommands[F[_], K] extends Flush[F, K] with Diagnostic[F]

trait Flush[F[_], K] {
  def keys(key: K): F[List[K]]
  def flushAll: F[Unit]
  def flushAll(mode: FlushMode): F[Unit]
  def flushDb: F[Unit]
  def flushDb(mode: FlushMode): F[Unit]
}

trait Diagnostic[F[_]] {
  def info: F[Map[String, String]]
  def info(section: String): F[Map[String, String]]
  def dbsize: F[Long]
  def lastSave: F[Instant]
  def slowLogLen: F[Long]
}
