package io.github.yisraelu.valkey4s.model

import cats.ApplicativeThrow
import glide.api.models.{configuration => G}
import scala.concurrent.duration.FiniteDuration

/** Configuration for standalone Valkey client */
sealed abstract class ValkeyClientConfig {

  /** List of server addresses (typically one for standalone) */
  def addresses: List[NodeAddress]

  /** Whether to use TLS/SSL encryption */
  def useTls: Boolean

  /** Optional timeout for requests */
  def requestTimeout: Option[FiniteDuration]

  /** Optional authentication credentials */
  def credentials: Option[ServerCredentials]

  /** Strategy for read operations in replication setups */
  def readFrom: Option[ReadFromStrategy]

  /** Reconnection backoff strategy */
  def reconnectStrategy: Option[BackOffStrategy]

  /** Optional database ID (0-15, default 0) */
  def databaseId: Option[Int]

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
      databaseId: Option[Int] = this.databaseId,
      clientName: Option[String] = this.clientName,
      protocolVersion: ProtocolVersion = this.protocolVersion,
      inflightRequestsLimit: Option[Int] = this.inflightRequestsLimit,
      connectionTimeout: Option[FiniteDuration] = this.connectionTimeout,
      libName: Option[String] = this.libName,
      lazyConnect: Option[Boolean] = this.lazyConnect,
      clientAZ: Option[String] = this.clientAZ
  ): ValkeyClientConfig =
    ValkeyClientConfig(
      addresses,
      useTls,
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
  private[valkey4s] def toGlide: G.GlideClientConfiguration = {
    val builder = G.GlideClientConfiguration.builder()

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
    databaseId.foreach(builder.databaseId(_))
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
      val advancedBuilder = G.AdvancedGlideClientConfiguration.builder()
      advancedBuilder.connectionTimeout(timeout.toMillis.toInt)
      builder.advancedConfiguration(advancedBuilder.build())
    }

    builder.build()
  }

  /** Add an address */
  def withAddress(
      host: String,
      port: Int = NodeAddress.DefaultPort
  ): ValkeyClientConfig =
    copy(addresses = List(NodeAddress(host, port)))

  def addAddress(nodeAddress: NodeAddress): ValkeyClientConfig =
    copy(addresses = addresses :+ nodeAddress)

  def addAddress(
      host: String,
      port: Int = NodeAddress.DefaultPort
  ): ValkeyClientConfig = addAddress(NodeAddress(host, port))

  /** Set TLS */
  def withTls(enabled: Boolean = true): ValkeyClientConfig =
    copy(useTls = enabled)

  def withTlsEnabled: ValkeyClientConfig =
    copy(useTls = true)

  def withTlsDisabled: ValkeyClientConfig = copy(useTls = false)

  /** Set request timeout */
  def withRequestTimeout(timeout: FiniteDuration): ValkeyClientConfig =
    copy(requestTimeout = Some(timeout))

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
  def withDatabase(db: Int): ValkeyClientConfig =
    copy(databaseId = Some(db))

  /** Set client name */
  def withClientName(name: String): ValkeyClientConfig =
    copy(clientName = Some(name))

  /** Set maximum number of concurrent in-flight requests (advanced performance tuning).
    *
    * This limits the number of concurrent requests that can be sent to the server
    * before waiting for responses. Useful for controlling memory usage and backpressure.
    *
    * Example:
    * {{{
    * config.withInflightRequestsLimit(1000) // Limit to 1000 concurrent requests
    * }}}
    */
  def withInflightRequestsLimit(limit: Int): ValkeyClientConfig =
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
  def withConnectionTimeout(timeout: FiniteDuration): ValkeyClientConfig =
    copy(connectionTimeout = Some(timeout))

  /** Set library name for client identification */
  def withLibName(name: String): ValkeyClientConfig =
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
  def withLazyConnectEnabled: ValkeyClientConfig =
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
  def withLazyConnectDisabled: ValkeyClientConfig =
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
  def withClientAZ(az: String): ValkeyClientConfig =
    copy(clientAZ = Some(az))
}

object ValkeyClientConfig {

  private final case class ValkeyClientConfigImpl(
      addresses: List[NodeAddress],
      useTls: Boolean = false,
      requestTimeout: Option[FiniteDuration] = None,
      credentials: Option[ServerCredentials] = None,
      readFrom: Option[ReadFromStrategy] = None,
      reconnectStrategy: Option[BackOffStrategy] = None,
      databaseId: Option[Int] = None,
      clientName: Option[String] = None,
      protocolVersion: ProtocolVersion = ProtocolVersion.RESP3,
      inflightRequestsLimit: Option[Int] = None,
      connectionTimeout: Option[FiniteDuration] = None,
      libName: Option[String] = None,
      lazyConnect: Option[Boolean] = None,
      clientAZ: Option[String] = None
  ) extends ValkeyClientConfig

  /** Create a ValkeyClientConfig */
  def apply(
      addresses: List[NodeAddress],
      useTls: Boolean = false,
      requestTimeout: Option[FiniteDuration] = None,
      credentials: Option[ServerCredentials] = None,
      readFrom: Option[ReadFromStrategy] = None,
      reconnectStrategy: Option[BackOffStrategy] = None,
      databaseId: Option[Int] = None,
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
      useTls,
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

  /** Pattern matching support */
  def unapply(config: ValkeyClientConfig): Option[
    (
        List[NodeAddress],
        Boolean,
        Option[FiniteDuration],
        Option[ServerCredentials],
        Option[ReadFromStrategy],
        Option[BackOffStrategy],
        Option[Int],
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
        config.databaseId,
        config.clientName,
        config.protocolVersion,
        config.inflightRequestsLimit,
        config.connectionTimeout,
        config.libName,
        config.lazyConnect,
        config.clientAZ
      )
    )

  /** Create configuration from a parsed ValkeyUri
    *
    * @param uri The parsed Valkey URI
    * @return Configuration based on the URI
    */
  def fromUri(uri: ValkeyUri): ValkeyClientConfig =
    ValkeyClientConfig(
      addresses = List(NodeAddress(uri.host, uri.port)),
      useTls = uri.useTls,
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

  /** Default config for localhost */
  val localhost: ValkeyClientConfig = ValkeyClientConfig(
    addresses = List(NodeAddress("localhost", NodeAddress.DefaultPort))
  )

  /** Builder-style constructor */
  def builder: ValkeyClientConfig = localhost
}
