package dev.profunktor.valkey4cats.model

import cats.{ApplicativeThrow, FlatMap}
import cats.syntax.all.*
import glide.api.models.configuration as G
import scala.concurrent.duration.FiniteDuration

/** Configuration for Valkey cluster client */
sealed abstract class ValkeyClusterConfig {

  /** List of seed node addresses (at least one required) */
  def addresses: List[NodeAddress]

  /** TLS encryption mode (disabled or enabled with optional advanced config) */
  def tlsMode: TlsMode

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

  /** Whether to refresh cluster topology from initial seed nodes (advanced) */
  def refreshTopologyFromInitialNodes: Option[Boolean]

  /** Create a copy with modified fields */
  private[model] def copy(
      addresses: List[NodeAddress] = this.addresses,
      tlsMode: TlsMode = this.tlsMode,
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
      clientAZ: Option[String] = this.clientAZ,
      refreshTopologyFromInitialNodes: Option[Boolean] =
        this.refreshTopologyFromInitialNodes
  ): ValkeyClusterConfig =
    ValkeyClusterConfig(
      addresses,
      tlsMode,
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
      clientAZ,
      refreshTopologyFromInitialNodes
    )

  /** Convert to Glide's GlideClusterClientConfiguration */
  private[valkey4cats] def toGlide: G.GlideClusterClientConfiguration = {
    val builder = G.GlideClusterClientConfiguration.builder()

    // Add all addresses
    addresses.foreach { addr =>
      builder.address(addr.toGlide)
    }

    // Set TLS
    val (useTls, tlsAdvancedConfig) = tlsMode.toGlide
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
    libName.foreach(builder.libName)
    lazyConnect.foreach(builder.lazyConnect)
    clientAZ.foreach(builder.clientAZ)

    // Build advanced configuration if any advanced settings are present
    if (
      connectionTimeout.isDefined || refreshTopologyFromInitialNodes.isDefined || tlsAdvancedConfig.isDefined
    ) {
      val advancedBuilder = G.AdvancedGlideClusterClientConfiguration.builder()
      connectionTimeout.foreach(timeout =>
        advancedBuilder.connectionTimeout(timeout.toMillis.toInt)
      )
      refreshTopologyFromInitialNodes.foreach(
        advancedBuilder.refreshTopologyFromInitialNodes
      )
      tlsAdvancedConfig.foreach(advancedBuilder.tlsAdvancedConfiguration)
      val _ = builder.advancedConfiguration(advancedBuilder.build())
    }

    builder.build()
  }

  /** Add a seed node address */
  def addAddress(
      host: String,
      port: Int = NodeAddress.DefaultPort
  ): ValkeyClusterConfig =
    copy(addresses = addresses :+ NodeAddress(host, port))

  /** Add a seed node address */
  def addAddress(nodeAddress: NodeAddress): ValkeyClusterConfig =
    copy(addresses = addresses :+ nodeAddress)

  /** Set TLS mode */
  def withTlsMode(mode: TlsMode): ValkeyClusterConfig =
    copy(tlsMode = mode)

  /** Enable TLS with default settings */
  def withTlsEnabled: ValkeyClusterConfig =
    copy(tlsMode = TlsMode.enabled)

  /** Enable TLS with advanced configuration */
  def withTlsAdvanced(config: TlsAdvancedConfig): ValkeyClusterConfig =
    copy(tlsMode = TlsMode.Enabled(Some(config)))

  /** Disable TLS encryption */
  def withTlsDisabled: ValkeyClusterConfig =
    copy(tlsMode = TlsMode.disabled)

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

  /** Set maximum number of concurrent in-flight requests (advanced performance tuning).
    *
    * This limits the number of concurrent requests that can be sent to the cluster
    * before waiting for responses. Useful for controlling memory usage and backpressure.
    *
    * Example:
    * {{{
    * config.withInflightRequestsLimit(1000) // Limit to 1000 concurrent requests
    * }}}
    */
  def withInflightRequestsLimit(limit: Int): ValkeyClusterConfig =
    copy(inflightRequestsLimit = Some(limit))

  /** Set connection timeout for establishing connections (distinct from request timeout).
    *
    * This timeout applies to the initial TCP connection handshake, while requestTimeout
    * applies to individual Redis commands after connection is established.
    *
    * Example:
    * {{{
    * config
    *   .withConnectionTimeout(10.seconds)  // TCP connection timeout
    *   .withRequestTimeout(5.seconds)      // Command execution timeout
    * }}}
    */
  def withConnectionTimeout(timeout: FiniteDuration): ValkeyClusterConfig =
    copy(connectionTimeout = Some(timeout))

  /** Set library name for client identification */
  def withLibName(name: String): ValkeyClusterConfig =
    copy(libName = Some(name))

  /** Enable lazy connection (defers connection until first operation).
    *
    * Lazy connection can reduce startup time but may cause the first operation to be slower.
    *
    * Example:
    * {{{
    * config.withLazyConnectEnabled // Connect on first operation (faster startup)
    * }}}
    */
  def withLazyConnectEnabled: ValkeyClusterConfig =
    copy(lazyConnect = Some(true))

  /** Disable lazy connection (establishes connection immediately on client creation).
    *
    * Eager connection is useful for fail-fast behavior and ensures the connection
    * is ready before the first operation.
    *
    * Example:
    * {{{
    * config.withLazyConnectDisabled // Connect immediately (fail-fast)
    * }}}
    */
  def withLazyConnectDisabled: ValkeyClusterConfig =
    copy(lazyConnect = Some(false))

  /** Set client availability zone for AZ-affinity read strategies.
    *
    * When combined with ReadFromStrategy.AzAffinity or AzAffinityReplicasAndPrimary,
    * this routes read requests to nodes in the same availability zone for lower latency.
    * Requires Valkey 8.0+ for AzAffinityReplicasAndPrimary strategy.
    *
    * Example:
    * {{{
    * config
    *   .withClientAZ("us-east-1a")
    *   .withReadFrom(ReadFromStrategy.AzAffinity)
    *   // or for Valkey 8.0+:
    *   .withReadFrom(ReadFromStrategy.AzAffinityReplicasAndPrimary)
    * }}}
    */
  def withClientAZ(az: String): ValkeyClusterConfig =
    copy(clientAZ = Some(az))

  /** Enable refreshing cluster topology from initial seed nodes (advanced).
    *
    * When enabled, the client will periodically refresh the cluster topology
    * by querying the initial seed nodes provided in the configuration,
    * rather than relying solely on CLUSTER SLOTS responses from connected nodes.
    *
    * This can be useful in scenarios where cluster topology changes frequently
    * or when you want to ensure the client always has an up-to-date view of the cluster.
    *
    * Example:
    * {{{
    * config.withRefreshTopologyFromInitialNodesEnabled
    * }}}
    */
  def withRefreshTopologyFromInitialNodesEnabled: ValkeyClusterConfig =
    copy(refreshTopologyFromInitialNodes = Some(true))

  /** Disable refreshing cluster topology from initial seed nodes.
    *
    * The client will only update topology information from CLUSTER SLOTS responses.
    *
    * Example:
    * {{{
    * config.withRefreshTopologyFromInitialNodesDisabled
    * }}}
    */
  def withRefreshTopologyFromInitialNodesDisabled: ValkeyClusterConfig =
    copy(refreshTopologyFromInitialNodes = Some(false))
}

object ValkeyClusterConfig {

  private final case class ValkeyClusterConfigImpl(
      addresses: List[NodeAddress],
      tlsMode: TlsMode = TlsMode.Disabled,
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
      clientAZ: Option[String] = None,
      refreshTopologyFromInitialNodes: Option[Boolean] = None
  ) extends ValkeyClusterConfig {
    require(
      addresses.nonEmpty,
      "At least one address is required for cluster configuration"
    )
  }

  /** Create a ValkeyClusterConfig */
  def apply(
      addresses: List[NodeAddress],
      tlsMode: TlsMode = TlsMode.Disabled,
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
      clientAZ: Option[String] = None,
      refreshTopologyFromInitialNodes: Option[Boolean] = None
  ): ValkeyClusterConfig =
    ValkeyClusterConfigImpl(
      addresses,
      tlsMode,
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
      clientAZ,
      refreshTopologyFromInitialNodes
    )

  /** Pattern matching support */
  def unapply(config: ValkeyClusterConfig): Option[
    (
        List[NodeAddress],
        TlsMode,
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
        Option[String],
        Option[Boolean]
    )
  ] =
    Some(
      (
        config.addresses,
        config.tlsMode,
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
        config.clientAZ,
        config.refreshTopologyFromInitialNodes
      )
    )

  /** Create from list of URI strings
    *
    * All URIs must have consistent settings (TLS and credentials).
    * If URIs have conflicting settings, an error will be raised.
    *
    * @param uris List of seed node URIs
    * @return Cluster configuration with validated consistent settings
    */
  def fromUris[F[_]: ApplicativeThrow: FlatMap](
      uris: List[String]
  ): F[ValkeyClusterConfig] = {
    if (uris.isEmpty) {
      ApplicativeThrow[F].raiseError(
        new IllegalArgumentException(
          "At least one URI is required for cluster configuration"
        )
      )
    } else {
      // Parse all URIs
      uris
        .traverse(uri =>
          ApplicativeThrow[F].fromEither(ValkeyUri.fromString(uri))
        )
        .flatMap { parsedUris =>
          // Validate consistency
          validateConsistentUris(parsedUris) match {
            case Left(error) =>
              ApplicativeThrow[F].raiseError(
                new IllegalArgumentException(error)
              )
            case Right(_) =>
              // All URIs are consistent, convert to configs and build cluster config
              val configs = parsedUris.map(ValkeyClientConfig.fromUri)
              val first = configs.head
              val allAddresses = configs.flatMap(_.addresses)

              ApplicativeThrow[F].pure(
                ValkeyClusterConfig(
                  addresses = allAddresses,
                  tlsMode = first.tlsMode,
                  requestTimeout = first.requestTimeout,
                  credentials = first.credentials,
                  readFrom = first.readFrom,
                  reconnectStrategy = first.reconnectStrategy,
                  clientName = first.clientName,
                  protocolVersion = first.protocolVersion
                )
              )
          }
        }
    }
  }

  /** Validate that all URIs have consistent settings
    *
    * Checks that TLS and credentials are consistent across all URIs using ValkeyUri.isConsistentWith.
    *
    * @param uris List of parsed URIs
    * @return Either an error message or Unit on success
    */
  private def validateConsistentUris(
      uris: List[ValkeyUri]
  ): Either[String, Unit] = {
    if (uris.isEmpty) {
      return Left("No URIs provided")
    }

    val first = uris.head
    val inconsistent = uris.tail.find(!first.isConsistentWith(_))

    inconsistent match {
      case Some(uri) =>
        // Determine which setting is inconsistent
        if (first.useTls != uri.useTls) {
          Left(
            s"Inconsistent TLS settings: some URIs use TLS (rediss://) and others don't (redis://). " +
              "All cluster seed nodes must have the same TLS setting."
          )
        } else {
          Left(
            "Inconsistent credentials: all cluster seed nodes must have the same authentication settings."
          )
        }
      case None => Right(())
    }
  }

  /** Builder-style constructor */
  def builder(
      host: String,
      port: Int = NodeAddress.DefaultPort
  ): ValkeyClusterConfig =
    ValkeyClusterConfig(addresses = List(NodeAddress(host, port)))
}
