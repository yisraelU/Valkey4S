package dev.profunktor.valkey4cats.algebra

import dev.profunktor.valkey4cats.model.ValkeyResponse

trait ConnectionCommands[F[_], K, V] {

  def ping: F[ValkeyResponse[String]]

  def ping(message: V): F[ValkeyResponse[V]]

  def echo(message: V): F[ValkeyResponse[V]]

  def clientId: F[ValkeyResponse[Long]]

  def clientGetName: F[ValkeyResponse[Option[String]]]

  def select(index: Long): F[ValkeyResponse[Unit]]
}
