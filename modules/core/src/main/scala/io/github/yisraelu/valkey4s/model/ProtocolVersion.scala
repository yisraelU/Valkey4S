package io.github.yisraelu.valkey4s.model

sealed abstract class ProtocolVersion(name: String) {
  override def toString: String = name
}

object ProtocolVersion {
  case object RESP2 extends ProtocolVersion("RESP2")
  case object RESP3 extends ProtocolVersion("RESP3")
}
