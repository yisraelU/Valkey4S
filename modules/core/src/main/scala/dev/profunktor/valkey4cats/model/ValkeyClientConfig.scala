package dev.profunktor.valkey4cats.model

import cats.ApplicativeThrow
import com.comcast.ip4s.{Host, Port}
import glide.api.models.configuration as G

import scala.concurrent.duration.{Duration, FiniteDuration}

/** Configuration for standalone Valkey client */
sealed abstract class ValkeyClientConfig {

  /** List of server addresses (typically one for standalone) */
  def addresses: List[NodeAddress]

  /** TLS encryption mode (disabled or enabled with optional advanced config) */
  def tlsMode: TlsMode

  /** Optional timeout for requests */
  def requestTimeout: Option[FiniteDuration]

  /** Optional authentication credentials */
  def credentials: Option[ServerCredentials]

  /** Strategy for read operations in replication setups */
  def readFrom: Option[ReadFromStrategy]

  /** Reconnection backoff strategy */
  def reconnectStrategy: Option[BackOffStrategy]

  /** Optional database ID (0-15, default 0) */
  def databaseId: Option[DatabaseId]

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
      tlsMode: TlsMode = this.tlsMode,
      requestTimeout: Option[FiniteDuration] = this.requestTimeout,
      credentials: Option[ServerCredentials] = this.credentials,
      readFrom: Option[ReadFromStrategy] = this.readFrom,
      reconnectStrategy: Option[BackOffStrategy] = this.reconnectStrategy,
      databaseId: Option[DatabaseId] = this.databaseId,
      clientName: Option[String] = this.clientName,
      protocolVersion: ProtocolVersion = this.protocolVersion,
      inflightRequestsLimit: Option[Int] = this.inflightRequestsLimit,
      connectionTimeout: Option[FiniteDuration] = this.connectionTimeout,
      libName: Option[String] = this.libName,
      lazyConnect: Option[Boolean] = this.lazyConnect,
      clientAZ: Option[String] = this.clientAZ
  ): ValkeyClientConfig =
    ValkeyClientConfig.unsafeCreate(
      addresses,
      tlsMode,
      requestTimeout,
      credentials,
      readFrom,
      reconnectStrategy,
      databaseId,
      clientName,
      protocolVersion,
      inflightRequestsLimit,
      connectionTimeout,
      libName,
      lazyConnect,
      clientAZ
    )

  /** Convert to Glide's GlideClientConfiguration */
  private[valkey4cats] def toGlide: G.GlideClientConfiguration = {
    val builder = G.GlideClientConfiguration.builder()

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
    databaseId.foreach(id => builder.databaseId(id.value))
    clientName.foreach(builder.clientName(_))

    // Protocol version
    builder.protocol(protocolVersion.toGlide)

    // Advanced configurations
    inflightRequestsLimit.foreach(builder.inflightRequestsLimit(_))
    libName.foreach(builder.libName(_))
    lazyConnect.foreach(builder.lazyConnect(_))
    clientAZ.foreach(builder.clientAZ(_))

    // Build advanced configuration if needed
    if (connectionTimeout.isDefined || tlsAdvancedConfig.isDefined) {
      val advancedBuilder = G.AdvancedGlideClientConfiguration.builder()
      connectionTimeout.foreach(timeout =>
        advancedBuilder.connectionTimeout(timeout.toMillis.toInt)
      )
      tlsAdvancedConfig.foreach(advancedBuilder.tlsAdvancedConfiguration)
      val _ = builder.advancedConfiguration(advancedBuilder.build())
    }

    builder.build()
  }

  /** Set address from ip4s types (always valid by construction) */
  def withAddress(
      host: Host,
      port: Port = NodeAddress.DefaultPort
  ): ValkeyClientConfig =
    copy(addresses = List(NodeAddress(host, port)))

  /** Set address from raw strings (validated) */
  def withAddress(host: String, port: Int): Either[String, ValkeyClientConfig] =
    NodeAddress.fromString(host, port).map(addr => copy(addresses = List(addr)))

  def addAddress(nodeAddress: NodeAddress): ValkeyClientConfig =
    copy(addresses = addresses :+ nodeAddress)

  /** Add address from ip4s types (always valid by construction) */
  def addAddress(host: Host, port: Port): ValkeyClientConfig =
    addAddress(NodeAddress(host, port))

  /** Add address from raw strings (validated) */
  def addAddress(host: String, port: Int): Either[String, ValkeyClientConfig] =
    NodeAddress.fromString(host, port).map(addAddress)

  /** Set TLS mode */
  def withTlsMode(mode: TlsMode): ValkeyClientConfig =
    copy(tlsMode = mode)

  /** Enable TLS with default settings */
  def withTlsEnabled: ValkeyClientConfig =
    copy(tlsMode = TlsMode.enabled)

  /** Enable TLS with advanced configuration */
  def withTlsAdvanced(config: TlsAdvancedConfig): ValkeyClientConfig =
    copy(tlsMode = TlsMode.Enabled(Some(config)))

  /** Disable TLS */
  def withTlsDisabled: ValkeyClientConfig =
    copy(tlsMode = TlsMode.disabled)

  /** Set request timeout */
  def withRequestTimeout(
      timeout: FiniteDuration
  ): Either[String, ValkeyClientConfig] =
    if (timeout > Duration.Zero) Right(copy(requestTimeout = Some(timeout)))
    else Left(s"Request timeout must be positive, got: $timeout")

  /** Set credentials */
  def withCredentials(creds: ServerCredentials): ValkeyClientConfig =
    copy(credentials = Some(creds))

  /** Set password authentication */
  def withPassword(password: String): ValkeyClientConfig =
    copy(credentials = Some(ServerCredentials.password(password)))

  /** Set read strategy */
  def withReadFrom(strategy: ReadFromStrategy): ValkeyClientConfig =
    copy(readFrom = Some(strategy))

  /** Set database ID */
  def withDatabase(db: Int): Either[String, ValkeyClientConfig] =
    DatabaseId(db).map(id => copy(databaseId = Some(id)))

  /** Set database ID from a validated DatabaseId */
  def withDatabase(db: DatabaseId): ValkeyClientConfig =
    copy(databaseId = Some(db))

  /** Set client name */
  def withClientName(name: String): ValkeyClientConfig =
    copy(clientName = Some(name))

  /** Set maximum number of concurrent in-flight requests (advanced performance tuning).
    *
    * This limits the number of concurrent requests that can be sent to the server
    * before waiting for responses. Useful for controlling memory usage and backpressure.
    */
  def withInflightRequestsLimit(
      limit: Int
  ): Either[String, ValkeyClientConfig] =
    if (limit > 0) Right(copy(inflightRequestsLimit = Some(limit)))
    else Left(s"Inflight requests limit must be positive, got: $limit")

  /** Set connection timeout for establishing connections (distinct from request timeout).
    *
    * This timeout applies to the initial TCP connection handshake, while requestTimeout
    * applies to individual Redis commands after connection is established.
    */
  def withConnectionTimeout(
      timeout: FiniteDuration
  ): Either[String, ValkeyClientConfig] =
    if (timeout > Duration.Zero) Right(copy(connectionTimeout = Some(timeout)))
    else Left(s"Connection timeout must be positive, got: $timeout")

  /** Set library name for client identification */
  def withLibName(name: String): ValkeyClientConfig =
    copy(libName = Some(name))

  /** Enable lazy connection (defers connection until first operation). */
  def withLazyConnectEnabled: ValkeyClientConfig =
    copy(lazyConnect = Some(true))

  /** Disable lazy connection (establishes connection immediately on client creation). */
  def withLazyConnectDisabled: ValkeyClientConfig =
    copy(lazyConnect = Some(false))

  /** Set client availability zone for AZ-affinity read strategies. */
  def withClientAZ(az: String): ValkeyClientConfig =
    copy(clientAZ = Some(az))
}

object ValkeyClientConfig {

  private final case class ValkeyClientConfigImpl(
      addresses: List[NodeAddress],
      tlsMode: TlsMode = TlsMode.Disabled,
      requestTimeout: Option[FiniteDuration] = None,
      credentials: Option[ServerCredentials] = None,
      readFrom: Option[ReadFromStrategy] = None,
      reconnectStrategy: Option[BackOffStrategy] = None,
      databaseId: Option[DatabaseId] = None,
      clientName: Option[String] = None,
      protocolVersion: ProtocolVersion = ProtocolVersion.RESP3,
      inflightRequestsLimit: Option[Int] = None,
      connectionTimeout: Option[FiniteDuration] = None,
      libName: Option[String] = None,
      lazyConnect: Option[Boolean] = None,
      clientAZ: Option[String] = None
  ) extends ValkeyClientConfig

  /** Create a ValkeyClientConfig with validation.
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
      databaseId: Option[DatabaseId] = None,
      clientName: Option[String] = None,
      protocolVersion: ProtocolVersion = ProtocolVersion.RESP3,
      inflightRequestsLimit: Option[Int] = None,
      connectionTimeout: Option[FiniteDuration] = None,
      libName: Option[String] = None,
      lazyConnect: Option[Boolean] = None,
      clientAZ: Option[String] = None
  ): Either[String, ValkeyClientConfig] = {
    val errors = List.newBuilder[String]
    if (addresses.isEmpty) errors += "At least one address is required"
    requestTimeout.foreach { t =>
      if (t <= Duration.Zero)
        errors += s"Request timeout must be positive, got: $t"
    }
    connectionTimeout.foreach { t =>
      if (t <= Duration.Zero)
        errors += s"Connection timeout must be positive, got: $t"
    }
    inflightRequestsLimit.foreach { l =>
      if (l <= 0) errors += s"Inflight requests limit must be positive, got: $l"
    }
    val errs = errors.result()
    if (errs.nonEmpty) Left(errs.mkString("; "))
    else
      Right(
        ValkeyClientConfigImpl(
          addresses,
          tlsMode,
          requestTimeout,
          credentials,
          readFrom,
          reconnectStrategy,
          databaseId,
          clientName,
          protocolVersion,
          inflightRequestsLimit,
          connectionTimeout,
          libName,
          lazyConnect,
          clientAZ
        )
      )
  }

  /** Internal unchecked constructor for use in copy and fromUri where inputs are already validated */
  private[model] def unsafeCreate(
      addresses: List[NodeAddress],
      tlsMode: TlsMode = TlsMode.Disabled,
      requestTimeout: Option[FiniteDuration] = None,
      credentials: Option[ServerCredentials] = None,
      readFrom: Option[ReadFromStrategy] = None,
      reconnectStrategy: Option[BackOffStrategy] = None,
      databaseId: Option[DatabaseId] = None,
      clientName: Option[String] = None,
      protocolVersion: ProtocolVersion = ProtocolVersion.RESP3,
      inflightRequestsLimit: Option[Int] = None,
      connectionTimeout: Option[FiniteDuration] = None,
      libName: Option[String] = None,
      lazyConnect: Option[Boolean] = None,
      clientAZ: Option[String] = None
  ): ValkeyClientConfig =
    ValkeyClientConfigImpl(
      addresses,
      tlsMode,
      requestTimeout,
      credentials,
      readFrom,
      reconnectStrategy,
      databaseId,
      clientName,
      protocolVersion,
      inflightRequestsLimit,
      connectionTimeout,
      libName,
      lazyConnect,
      clientAZ
    )

  /** Create configuration from a parsed ValkeyUri
    *
    * @param uri The parsed Valkey URI
    * @return Configuration based on the URI
    */
  def fromUri(uri: ValkeyUri): ValkeyClientConfig =
    unsafeCreate(
      addresses =
        List(NodeAddress(uri.host, uri.port)), // ip4s types from ValkeyUri
      tlsMode = if (uri.useTls) TlsMode.enabled else TlsMode.disabled,
      credentials = uri.credentials,
      databaseId = uri.database
    )

  /** Parse from URI string
    *
    * Supports formats:
    * - valkey://host:port (native Valkey scheme)
    * - valkeys://host:port (native Valkey scheme with TLS)
    * - redis://host:port (legacy compatibility)
    * - rediss://host:port (legacy compatibility with TLS)
    * - valkey://:password@host:port
    * - valkey://username:password@host:port
    * - valkey://host:port/db
    *
    * @param uriString The URI string to parse
    * @return Either an error or the parsed config
    */
  def fromUriString(uriString: String): Either[Throwable, ValkeyClientConfig] =
    ValkeyUri.fromString(uriString).map(fromUri)

  /** Parse from URI string with effect handling
    *
    * @param uri The URI string to parse
    * @return Config wrapped in effect F
    */
  def fromUri[F[_]: ApplicativeThrow](uri: String): F[ValkeyClientConfig] =
    ApplicativeThrow[F].fromEither(fromUriString(uri))

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
      databaseId: Option[DatabaseId] = None,
      clientName: Option[String] = None,
      protocolVersion: ProtocolVersion = ProtocolVersion.RESP3,
      inflightRequestsLimit: Option[Int] = None,
      connectionTimeout: Option[FiniteDuration] = None,
      libName: Option[String] = None,
      lazyConnect: Option[Boolean] = None,
      clientAZ: Option[String] = None
  ): F[ValkeyClientConfig] =
    ApplicativeThrow[F].fromEither(
      apply(
        addresses,
        tlsMode,
        requestTimeout,
        credentials,
        readFrom,
        reconnectStrategy,
        databaseId,
        clientName,
        protocolVersion,
        inflightRequestsLimit,
        connectionTimeout,
        libName,
        lazyConnect,
        clientAZ
      ).left.map(msg => new IllegalArgumentException(msg))
    )

  private val localhostHost: Host =
    Host.fromString("localhost").get // Safe: "localhost" is always valid

  /** Default config for localhost */
  val localhost: ValkeyClientConfig = unsafeCreate(
    addresses = List(NodeAddress(localhostHost, NodeAddress.DefaultPort))
  )

  /** Builder-style constructor */
  def builder: ValkeyClientConfig = localhost
}
