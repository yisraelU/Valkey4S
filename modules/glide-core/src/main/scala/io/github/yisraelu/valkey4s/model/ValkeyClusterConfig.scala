package io.github.yisraelu.valkey4s.model

import cats.ApplicativeThrow
import cats.syntax.all.*
import glide.api.models.configuration as G
import scala.concurrent.duration.FiniteDuration

/** Configuration for Valkey cluster client */
sealed abstract class ValkeyClusterConfig {

  /** List of seed node addresses (at least one required) */
  def addresses: List[NodeAddress]

  /** Whether to use TLS/SSL encryption */
  def useTls: Boolean

  /** Optional timeout for requests */
  def requestTimeout: Option[FiniteDuration]

  /** Optional authentication credentials */
  def credentials: Option[ServerCredentials]

  /** Strategy for read operations */
  def readFrom: Option[ReadFromStrategy]

  /** Reconnection backoff strategy */
  def reconnectStrategy: Option[BackOffStrategy]

  /** Optional client name for debugging */
  def clientName: Option[String]

  /** RESP protocol version (RESP2 or RESP3) */
  def protocolVersion: ProtocolVersion

  /** Maximum number of concurrent in-flight requests (advanced) */
  def inflightRequestsLimit: Option[Int]

  /** Connection timeout for establishing connections (advanced) */
  def connectionTimeout: Option[FiniteDuration]

  /** Library name for client identification */
  def libName: Option[String]

  /** Whether to connect lazily (on first operation) or eagerly (on client creation) */
  def lazyConnect: Option[Boolean]

  /** Client availability zone for AZ-affinity reads */
  def clientAZ: Option[String]

  /** Create a copy with modified fields */
  private[model] def copy(
      addresses: List[NodeAddress] = this.addresses,
      useTls: Boolean = this.useTls,
      requestTimeout: Option[FiniteDuration] = this.requestTimeout,
      credentials: Option[ServerCredentials] = this.credentials,
      readFrom: Option[ReadFromStrategy] = this.readFrom,
      reconnectStrategy: Option[BackOffStrategy] = this.reconnectStrategy,
      clientName: Option[String] = this.clientName,
      protocolVersion: ProtocolVersion = this.protocolVersion,
      inflightRequestsLimit: Option[Int] = this.inflightRequestsLimit,
      connectionTimeout: Option[FiniteDuration] = this.connectionTimeout,
      libName: Option[String] = this.libName,
      lazyConnect: Option[Boolean] = this.lazyConnect,
      clientAZ: Option[String] = this.clientAZ
  ): ValkeyClusterConfig =
    ValkeyClusterConfig(
      addresses,
      useTls,
      requestTimeout,
      credentials,
      readFrom,
      reconnectStrategy,
      clientName,
      protocolVersion,
      inflightRequestsLimit,
      connectionTimeout,
      libName,
      lazyConnect,
      clientAZ
    )

  /** Convert to Glide's GlideClusterClientConfiguration */
  private[valkey4s] def toGlide: G.GlideClusterClientConfiguration = {
    val builder = G.GlideClusterClientConfiguration.builder()

    // Add all addresses
    addresses.foreach { addr =>
      builder.address(addr.toGlide)
    }

    // Set TLS
    builder.useTLS(useTls)

    // Optional configurations
    requestTimeout.foreach(d => builder.requestTimeout(d.toMillis.toInt))
    credentials.foreach(c => builder.credentials(c.toGlide))
    readFrom.foreach(r => builder.readFrom(r.toGlide))
    reconnectStrategy.foreach(s => builder.reconnectStrategy(s.toGlide))
    clientName.foreach(builder.clientName(_))

    // Protocol version
    builder.protocol(protocolVersion.toGlide)

    // Advanced configurations
    inflightRequestsLimit.foreach(builder.inflightRequestsLimit(_))
    libName.foreach(builder.libName(_))
    lazyConnect.foreach(builder.lazyConnect(_))
    clientAZ.foreach(builder.clientAZ(_))

    // Build advanced configuration if needed
    connectionTimeout.foreach { timeout =>
      val advancedBuilder = G.AdvancedGlideClusterClientConfiguration.builder()
      advancedBuilder.connectionTimeout(timeout.toMillis.toInt)
      builder.advancedConfiguration(advancedBuilder.build())
    }

    builder.build()
  }

  /** Add a seed node address */
  def withAddress(
      host: String,
      port: Int = NodeAddress.DefaultPort
  ): ValkeyClusterConfig =
    copy(addresses = addresses :+ NodeAddress(host, port))

  /** Set TLS */
  def withTls(enabled: Boolean = true): ValkeyClusterConfig =
    copy(useTls = enabled)

  /** Set request timeout */
  def withRequestTimeout(timeout: FiniteDuration): ValkeyClusterConfig =
    copy(requestTimeout = Some(timeout))

  /** Set credentials */
  def withCredentials(creds: ServerCredentials): ValkeyClusterConfig =
    copy(credentials = Some(creds))

  /** Set password authentication */
  def withPassword(password: String): ValkeyClusterConfig =
    copy(credentials = Some(ServerCredentials.password(password)))

  /** Set read strategy */
  def withReadFrom(strategy: ReadFromStrategy): ValkeyClusterConfig =
    copy(readFrom = Some(strategy))

  /** Set reconnection backoff strategy */
  def withReconnectStrategy(strategy: BackOffStrategy): ValkeyClusterConfig =
    copy(reconnectStrategy = Some(strategy))

  /** Set client name */
  def withClientName(name: String): ValkeyClusterConfig =
    copy(clientName = Some(name))

  /** Set maximum number of concurrent in-flight requests */
  def withInflightRequestsLimit(limit: Int): ValkeyClusterConfig =
    copy(inflightRequestsLimit = Some(limit))

  /** Set connection timeout for establishing connections */
  def withConnectionTimeout(timeout: FiniteDuration): ValkeyClusterConfig =
    copy(connectionTimeout = Some(timeout))

  /** Set library name for client identification */
  def withLibName(name: String): ValkeyClusterConfig =
    copy(libName = Some(name))

  /** Set whether to connect lazily (on first operation) or eagerly (on client creation) */
  def withLazyConnect(enabled: Boolean = true): ValkeyClusterConfig =
    copy(lazyConnect = Some(enabled))

  /** Set client availability zone for AZ-affinity reads */
  def withClientAZ(az: String): ValkeyClusterConfig =
    copy(clientAZ = Some(az))
}

object ValkeyClusterConfig {

  private final case class ValkeyClusterConfigImpl(
      addresses: List[NodeAddress],
      useTls: Boolean = false,
      requestTimeout: Option[FiniteDuration] = None,
      credentials: Option[ServerCredentials] = None,
      readFrom: Option[ReadFromStrategy] = None,
      reconnectStrategy: Option[BackOffStrategy] = None,
      clientName: Option[String] = None,
      protocolVersion: ProtocolVersion = ProtocolVersion.RESP3,
      inflightRequestsLimit: Option[Int] = None,
      connectionTimeout: Option[FiniteDuration] = None,
      libName: Option[String] = None,
      lazyConnect: Option[Boolean] = None,
      clientAZ: Option[String] = None
  ) extends ValkeyClusterConfig {
    require(
      addresses.nonEmpty,
      "At least one address is required for cluster configuration"
    )
  }

  /** Create a ValkeyClusterConfig */
  def apply(
      addresses: List[NodeAddress],
      useTls: Boolean = false,
      requestTimeout: Option[FiniteDuration] = None,
      credentials: Option[ServerCredentials] = None,
      readFrom: Option[ReadFromStrategy] = None,
      reconnectStrategy: Option[BackOffStrategy] = None,
      clientName: Option[String] = None,
      protocolVersion: ProtocolVersion = ProtocolVersion.RESP3,
      inflightRequestsLimit: Option[Int] = None,
      connectionTimeout: Option[FiniteDuration] = None,
      libName: Option[String] = None,
      lazyConnect: Option[Boolean] = None,
      clientAZ: Option[String] = None
  ): ValkeyClusterConfig =
    ValkeyClusterConfigImpl(
      addresses,
      useTls,
      requestTimeout,
      credentials,
      readFrom,
      reconnectStrategy,
      clientName,
      protocolVersion,
      inflightRequestsLimit,
      connectionTimeout,
      libName,
      lazyConnect,
      clientAZ
    )

  /** Pattern matching support */
  def unapply(config: ValkeyClusterConfig): Option[
    (
        List[NodeAddress],
        Boolean,
        Option[FiniteDuration],
        Option[ServerCredentials],
        Option[ReadFromStrategy],
        Option[BackOffStrategy],
        Option[String],
        ProtocolVersion,
        Option[Int],
        Option[FiniteDuration],
        Option[String],
        Option[Boolean],
        Option[String]
    )
  ] =
    Some(
      (
        config.addresses,
        config.useTls,
        config.requestTimeout,
        config.credentials,
        config.readFrom,
        config.reconnectStrategy,
        config.clientName,
        config.protocolVersion,
        config.inflightRequestsLimit,
        config.connectionTimeout,
        config.libName,
        config.lazyConnect,
        config.clientAZ
      )
    )

  /** Create from list of URI strings */
  def fromUris[F[_]: ApplicativeThrow](
      uris: List[String]
  ): F[ValkeyClusterConfig] = {
    if (uris.isEmpty) {
      ApplicativeThrow[F].raiseError(
        new IllegalArgumentException(
          "At least one URI is required for cluster configuration"
        )
      )
    } else {
      uris.traverse(ValkeyClientConfig.fromUri[F]).map { configs =>
        // Extract addresses from all configs
        val allAddresses = configs.flatMap(_.addresses)

        // Use settings from first config
        val first = configs.head
        ValkeyClusterConfig(
          addresses = allAddresses,
          useTls = first.useTls,
          requestTimeout = first.requestTimeout,
          credentials = first.credentials,
          readFrom = first.readFrom,
          reconnectStrategy = first.reconnectStrategy,
          clientName = first.clientName,
          protocolVersion = first.protocolVersion
        )
      }
    }
  }

  /** Builder-style constructor */
  def builder(
      host: String,
      port: Int = NodeAddress.DefaultPort
  ): ValkeyClusterConfig =
    ValkeyClusterConfig(addresses = List(NodeAddress(host, port)))
}
