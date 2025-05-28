

package io.github.yisrael.valkey4s.algebra

trait ConnectionCommands[F[_], K] extends Ping[F] with Auth[F] with Client[F, K]

trait Ping[F[_]] {
  def ping: F[String]
  def select(index: Int): F[Unit]
}

trait Auth[F[_]] {
  def auth(password: CharSequence): F[Boolean]
  def auth(username: String, password: CharSequence): F[Boolean]
}

trait Client[F[_], K] {
  def setClientName(name: K): F[Boolean]
  def getClientName(): F[Option[K]]
  def getClientId(): F[Long]
  def getClientInfo: F[Map[String, String]]
  def setLibName(name: String): F[Boolean]
  def setLibVersion(version: String): F[Boolean]
}
