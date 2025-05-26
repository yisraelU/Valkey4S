package io.github.yisraelu.valkey4s.model

sealed abstract class ReadFromStrategy(val name: String) {
  override def toString: String = name
}

object ReadFromStrategy {

 /**
  *  Always get from primary, in order to get the freshest data.
  *  */
 
  case object PRIMARY extends ReadFromStrategy("PRIMARY")
  /**
   * Spread the requests between all replicas in a round-robin manner. If no replica is available,
   * route the requests to the primary.
   */
  case object PREFER_REPLICA extends ReadFromStrategy("PREFER_REPLICA")
  /**
   * Spread the read requests between replicas in the same client's AZ (Aviliablity zone) in a
   * round-robin manner, falling back to other replicas or the primary if needed.
   */
  case object AZ_AFFINITY extends ReadFromStrategy("AZ_AFFINITY")
  /**
   * Spread the read requests among nodes within the client's Availability Zone (AZ) in a round
   * robin manner, prioritizing local replicas, then the local primary, and falling back to any
   * replica or the primary if needed.
   */
  case object AZ_AFFINITY_REPLICAS_AND_PRIMARY extends ReadFromStrategy("AZ_AFFINITY_REPLICAS_AND_PRIMARY")
}
