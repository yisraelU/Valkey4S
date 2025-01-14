package dev.profunktor.valkey4cats.results

final case class PendingSummary[K](
    pendingCount: Long,
    smallestId: Option[String],
    greatestId: Option[String],
    consumers: List[PendingSummary.ConsumerPending[K]]
)

object PendingSummary {
  final case class ConsumerPending[K](consumer: K, pendingCount: Long)
}

final case class PendingEntry[K](
    messageId: String,
    consumer: K,
    idleTimeMillis: Long,
    deliveryCount: Long
)

final case class AutoClaimResult[K, V](
    nextCursor: String,
    claimedEntries: Map[String, List[(K, V)]],
    deletedIds: List[String]
)

final case class AutoClaimIdResult(
    nextCursor: String,
    claimedIds: List[String],
    deletedIds: List[String]
)
