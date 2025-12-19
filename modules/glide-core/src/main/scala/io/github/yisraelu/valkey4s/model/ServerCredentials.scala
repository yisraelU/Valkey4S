package io.github.yisraelu.valkey4s.model

import glide.api.models.{configuration => G}

/** Authentication credentials for Valkey server
  *
  * Supports both password-only (Redis 5.0+) and username+password (Redis 6.0+/Valkey)
  */
sealed abstract class ServerCredentials { self =>

  private[valkey4s] def toGlide: G.ServerCredentials =
    self match {
      case p: ServerCredentials.Password =>
        G.ServerCredentials
          .builder()
          .password(p.password)
          .build()
      case up: ServerCredentials.UsernamePassword =>
        G.ServerCredentials
          .builder()
          .username(up.username)
          .password(up.password)
          .build()
    }
}

object ServerCredentials {

  /** Password-only authentication (Redis 5.0+) */
  sealed abstract class Password extends ServerCredentials {
    def password: String
  }

  object Password {
    private final class PasswordImpl(val password: String) extends Password

    def apply(password: String): Password = new PasswordImpl(password)

    def unapply(p: Password): Option[String] = Some(p.password)
  }

  /** Username and password authentication (Redis 6.0+/Valkey) */
  sealed abstract class UsernamePassword extends ServerCredentials {
    def username: String
    def password: String
  }

  object UsernamePassword {
    private final class UsernamePasswordImpl(
        val username: String,
        val password: String
    ) extends UsernamePassword

    def apply(username: String, password: String): UsernamePassword =
      new UsernamePasswordImpl(username, password)

    def unapply(up: UsernamePassword): Option[(String, String)] =
      Some((up.username, up.password))
  }

  /** Convenience constructor for password-only auth */
  def password(pwd: String): ServerCredentials = Password(pwd)

  /** Convenience constructor for username+password auth */
  def usernamePassword(user: String, pwd: String): ServerCredentials =
    UsernamePassword(user, pwd)
}
