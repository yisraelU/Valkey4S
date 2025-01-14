package dev.profunktor.valkey4cats.syntax

import cats.MonadThrow
import cats.syntax.all.*
import dev.profunktor.valkey4cats.model.ValkeyResponse

object response {
  implicit class ValkeyResponseOps[F[_], A](
      private val fa: F[ValkeyResponse[A]]
  ) extends AnyVal {
    def direct(implicit F: MonadThrow[F]): F[A] =
      F.flatMap(fa)(_.liftTo[F, A])
  }
}
