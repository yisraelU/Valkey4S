package io.github.yisraelu.valkey4s.model

import glide.api.models.{configuration => G}

/** Redis/Valkey protocol version */
sealed trait ProtocolVersion { self =>

  private[valkey4s] def toGlide: G.ProtocolVersion =
    self match {
      case ProtocolVersion.RESP2 => G.ProtocolVersion.RESP2
      case ProtocolVersion.RESP3 => G.ProtocolVersion.RESP3
    }
}

object ProtocolVersion {

  /** RESP2 protocol (older, compatible with all Redis versions) */
  case object RESP2 extends ProtocolVersion

  /** RESP3 protocol (Redis 6.0+/Valkey, better performance and features) */
  case object RESP3 extends ProtocolVersion
}
