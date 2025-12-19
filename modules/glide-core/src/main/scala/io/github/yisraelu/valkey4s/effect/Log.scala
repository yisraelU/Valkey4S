package io.github.yisraelu.valkey4s.effect

import cats.effect.Sync

/** Simple logging abstraction */
trait Log[F[_]] {
  def info(msg: => String): F[Unit]
  def error(msg: => String): F[Unit]
  def debug(msg: => String): F[Unit]
}

object Log {
  def apply[F[_]](implicit ev: Log[F]): Log[F] = ev

  /** No-op logger */
  implicit def noOp[F[_]: Sync]: Log[F] = new Log[F] {
    def info(msg: => String): F[Unit] = Sync[F].unit
    def error(msg: => String): F[Unit] = Sync[F].unit
    def debug(msg: => String): F[Unit] = Sync[F].unit
  }

  /** Console logger implementations */
  object Stdout {
    implicit def instance[F[_]: Sync]: Log[F] = new Log[F] {
      def info(msg: => String): F[Unit] =
        Sync[F].delay(println(s"[INFO] $msg"))

      def error(msg: => String): F[Unit] =
        Sync[F].delay(System.err.println(s"[ERROR] $msg"))

      def debug(msg: => String): F[Unit] =
        Sync[F].delay(println(s"[DEBUG] $msg"))
    }
  }
}
