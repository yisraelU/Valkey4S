package io.github.yisraelu.valkey4s.connection

import cats.ApplicativeThrow
import cats.implicits.toBifunctorOps

import scala.util.Try
import scala.util.control.NoStackTrace

sealed abstract class GlideUri private(val underlying: JRedisURI)

object GlideUri {
  def make[F[_]: ApplicativeThrow](uri: => String): F[GlideUri] =
    ApplicativeThrow[F].catchNonFatal(new GlideUri(JRedisURI.create(uri)) {})

  def fromUnderlying(j: JRedisURI): GlideUri = new GlideUri(j) {}

  def fromString(uri: String): Either[InvalidRedisURI, GlideUri] =
    Try(JRedisURI.create(uri)).toEither.bimap(InvalidRedisURI(uri, _), new GlideUri(_) {})

  def unsafeFromString(uri: String): GlideUri = new GlideUri(JRedisURI.create(uri)) {}
}

final case class InvalidRedisURI(uri: String, throwable: Throwable) extends NoStackTrace {
  override def getMessage: String = Option(throwable.getMessage).getOrElse(s"Invalid Glide URI: $uri")
}
