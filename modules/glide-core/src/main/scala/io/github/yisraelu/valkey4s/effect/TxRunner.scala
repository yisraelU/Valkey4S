package io.github.yisraelu.valkey4s.effect

import cats.effect._

/** Transaction runner for MULTI/EXEC support
  *
  * This is a stub for Phase 1 - full implementation coming in Phase 3
  */
trait TxRunner[F[_]] {
  // TODO: Implement transaction support in Phase 3
}

object TxRunner {

  /** Create a no-op transaction runner for Phase 1 */
  def make[F[_]: Async]: F[TxRunner[F]] =
    Async[F].pure(new TxRunner[F] {})
}
