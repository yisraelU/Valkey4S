package dev.profunktor.valkey4cats.model

import cats.{ApplicativeThrow, FlatMap}
import cats.syntax.all.*
import com.comcast.ip4s.{Host, Port}
import glide.api.models.configuration as G
import scala.concurrent.duration.{Duration, FiniteDuration}

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
    ValkeyClusterConfig.unsafeCreate(
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

  /** Add a seed node address from ip4s types (always valid by construction) */
  def addAddress(host: Host, port: Port): ValkeyClusterConfig =
    addAddress(NodeAddress(host, port))

  /** Add a seed node address from raw strings (validated) */
  def addAddress(host: String, port: Int): Either[String, ValkeyClusterConfig] =
    NodeAddress.fromString(host, port).map(addAddress)

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
  def withRequestTimeout(
      timeout: FiniteDuration
  ): Either[String, ValkeyClusterConfig] =
    if (timeout > Duration.Zero) Right(copy(requestTimeout = Some(timeout)))
    else Left(s"Request timeout must be positive, got: $timeout")

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

  /** Set maximum number of concurrent in-flight requests (advanced performance tuning). */
  def withInflightRequestsLimit(
      limit: Int
  ): Either[String, ValkeyClusterConfig] =
    if (limit > 0) Right(copy(inflightRequestsLimit = Some(limit)))
    else Left(s"Inflight requests limit must be positive, got: $limit")

  /** Set connection timeout for establishing connections (distinct from request timeout). */
  def withConnectionTimeout(
      timeout: FiniteDuration
  ): Either[String, ValkeyClusterConfig] =
    if (timeout > Duration.Zero) Right(copy(connectionTimeout = Some(timeout)))
    else Left(s"Connection timeout must be positive, got: $timeout")

  /** Set library name for client identification */
  def withLibName(name: String): ValkeyClusterConfig =
    copy(libName = Some(name))

  /** Enable lazy connection (defers connection until first operation). */
  def withLazyConnectEnabled: ValkeyClusterConfig =
    copy(lazyConnect = Some(true))

  /** Disable lazy connection (establishes connection immediately on client creation). */
  def withLazyConnectDisabled: ValkeyClusterConfig =
    copy(lazyConnect = Some(false))

  /** Set client availability zone for AZ-affinity read strategies. */
  def withClientAZ(az: String): ValkeyClusterConfig =
    copy(clientAZ = Some(az))

  /** Enable refreshing cluster topology from initial seed nodes (advanced). */
  def withRefreshTopologyFromInitialNodesEnabled: ValkeyClusterConfig =
    copy(refreshTopologyFromInitialNodes = Some(true))

  /** Disable refreshing cluster topology from initial seed nodes. */
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
  ) extends ValkeyClusterConfig

  /** Create a ValkeyClusterConfig with validation.
    *
    * @return Either an error message or the validated config
    */
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
  ): Either[String, ValkeyClusterConfig] = {
    val errors = List.newBuilder[String]
    if (addresses.isEmpty)
      errors += "At least one address is required for cluster configuration"
    requestTimeout.foreach { t =>
      if (t <= Duration.Zero)
        errors += s"Request timeout must be positive, got: $t"
    }
    connectionTimeout.foreach { t =>
      if (t <= Duration.Zero)
        errors += s"Connection timeout must be positive, got: $t"
    }
    inflightRequestsLimit.foreach { l =>
      if (l <= 0)
        errors += s"Inflight requests limit must be positive, got: $l"
    }
    val errs = errors.result()
    if (errs.nonEmpty) Left(errs.mkString("; "))
    else
      Right(
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
      )
  }

  /** Internal unchecked constructor for use in copy and fromUris where inputs are already validated */
  private[model] def unsafeCreate(
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
                unsafeCreate(
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

  /** Validate that all URIs have consistent settings */
  private def validateConsistentUris(
      uris: List[ValkeyUri]
  ): Either[String, Unit] = {
    if (uris.isEmpty) Left("No URIs provided")
    else {
      val first = uris.head
      val inconsistent = uris.tail.find(!first.isConsistentWith(_))

      inconsistent match {
        case Some(uri) =>
          if (first.useTls != uri.useTls)
            Left(
              s"Inconsistent TLS settings: some URIs use TLS (rediss://) and others don't (redis://). " +
                "All cluster seed nodes must have the same TLS setting."
            )
          else
            Left(
              "Inconsistent credentials: all cluster seed nodes must have the same authentication settings."
            )
        case None => Right(())
      }
    }
  }

  /** Effectful smart constructor that lifts validation errors into F.
    *
    * @return Config wrapped in effect F, raising on validation failure
    */
  def make[F[_]: ApplicativeThrow](
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
  ): F[ValkeyClusterConfig] =
    ApplicativeThrow[F].fromEither(
      apply(
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
      ).left.map(msg => new IllegalArgumentException(msg))
    )

  /** Builder-style constructor from ip4s types (always valid by construction) */
  def builder(
      host: Host,
      port: Port = NodeAddress.DefaultPort
  ): ValkeyClusterConfig =
    unsafeCreate(addresses = List(NodeAddress(host, port)))

  /** Builder-style constructor from raw strings (validated) */
  def builder(host: String, port: Int): Either[String, ValkeyClusterConfig] =
    NodeAddress
      .fromString(host, port)
      .map(addr => unsafeCreate(addresses = List(addr)))
}
