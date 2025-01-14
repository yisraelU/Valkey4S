package dev.profunktor.valkey4cats.results

final case class ScanResult[A](cursor: String, values: A)

final case class ClusterScanResult[A](
    cursor: ClusterScanCursor,
    values: A
)

sealed trait ClusterScanCursor {
  def isFinished: Boolean
}

object ClusterScanCursor {
  private[valkey4cats] def initial: ClusterScanCursor =
    Wrapped(
      glide.api.models.commands.scan.ClusterScanCursor.initialCursor()
    )

  private[valkey4cats] final case class Wrapped(
      underlying: glide.api.models.commands.scan.ClusterScanCursor
  ) extends ClusterScanCursor {
    def isFinished: Boolean = underlying.isFinished
  }
}
