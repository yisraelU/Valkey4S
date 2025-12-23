package dev.profunktor.valkey4cats.model

import glide.api.models.{configuration => G}

/** Represents a Valkey server node address */
sealed abstract class NodeAddress {

  /** The hostname or IP address */
  def host: String

  /** The port number */
  def port: Int

  /** Convert to Glide's NodeAddress */
  private[valkey4cats] def toGlide: G.NodeAddress =
    G.NodeAddress
      .builder()
      .host(host)
      .port(port)
      .build()
}

object NodeAddress {

  /** Default Valkey port */
  val DefaultPort: Int = 6379

  private final case class NodeAddressImpl(
      host: String,
      port: Int
  ) extends NodeAddress

  /** Create a NodeAddress
    *
    * @param host The hostname or IP address
    * @param port The port number
    */
  def apply(host: String, port: Int): NodeAddress =
    NodeAddressImpl(host, port)

  /** Create a NodeAddress with default port */
  def apply(host: String): NodeAddress =
    NodeAddressImpl(host, DefaultPort)

  /** Pattern matching support */
  def unapply(addr: NodeAddress): Option[(String, Int)] =
    Some((addr.host, addr.port))
}
