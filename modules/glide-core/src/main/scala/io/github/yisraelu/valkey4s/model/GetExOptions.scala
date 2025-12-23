package io.github.yisraelu.valkey4s.model

import glide.api.models.{commands => G}

/** Expiry options for GETEX command */
sealed trait GetExExpiry {
  private[valkey4s] def toGlide: G.GetExOptions
}

object GetExExpiry {
  /** Set expiry in seconds */
  final case class Seconds(seconds: Long) extends GetExExpiry {
    override private[valkey4s] def toGlide: G.GetExOptions =
      G.GetExOptions.Seconds(seconds)
  }

  /** Set expiry in milliseconds */
  final case class Milliseconds(milliseconds: Long) extends GetExExpiry {
    override private[valkey4s] def toGlide: G.GetExOptions =
      G.GetExOptions.Milliseconds(milliseconds)
  }

  /** Set expiry as Unix timestamp in seconds */
  final case class UnixSeconds(timestamp: Long) extends GetExExpiry {
    override private[valkey4s] def toGlide: G.GetExOptions =
      G.GetExOptions.UnixSeconds(timestamp)
  }

  /** Set expiry as Unix timestamp in milliseconds */
  final case class UnixMilliseconds(timestamp: Long) extends GetExExpiry {
    override private[valkey4s] def toGlide: G.GetExOptions =
      G.GetExOptions.UnixMilliseconds(timestamp)
  }

  /** Remove the time to live associated with the key (PERSIST) */
  case object Persist extends GetExExpiry {
    override private[valkey4s] def toGlide: G.GetExOptions =
      G.GetExOptions.Persist()
  }
}
