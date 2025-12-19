package io.github.yisraelu.valkey4s.model

import glide.api.models.{configuration => G}
import io.github.yisraelu.valkey4s.model.ReadFromStrategy.*

/** Strategy for read operations in cluster/replication setups */
sealed trait ReadFromStrategy { self =>
  private[valkey4s] def toGlide: G.ReadFrom = self match {
    case Primary       => G.ReadFrom.PRIMARY
    case PreferReplica => G.ReadFrom.PREFER_REPLICA
    case AzAffinity    => G.ReadFrom.AZ_AFFINITY
  }
}

object ReadFromStrategy {

  /** Read from primary only (default) */
  case object Primary extends ReadFromStrategy

  /** Prefer reading from replicas, fallback to primary */
  case object PreferReplica extends ReadFromStrategy

  /** AZ Affinity: prefer reading from nodes in the same availability zone
    * This is a Glide-specific feature for optimal latency in cloud deployments
    */
  case object AzAffinity extends ReadFromStrategy

}
