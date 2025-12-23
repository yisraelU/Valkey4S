package io.github.yisraelu.valkey4s.connection

import cats.effect.*
import cats.syntax.all.*
import glide.api.GlideClient
import io.github.yisraelu.valkey4s.effect.Log
import io.github.yisraelu.valkey4s.model.{ValkeyClientConfig, ValkeyUri}

/** Wrapper around Glide's standalone client with functional resource management
  *
  * @param underlying The underlying GlideClient instance
  */
sealed abstract class ValkeyClient[F[_]] private (
    val underlying: GlideClient
)

object ValkeyClient {

  /** Private implementation class */
  private final case class ValkeyClientImpl[F[_]](
      override val underlying: GlideClient
  ) extends ValkeyClient[F](underlying)

  /** Create a ValkeyClient from configuration with Resource management
    *
    * @param config The client configuration
    * @return Resource that manages the client lifecycle
    */
  def fromConfig[F[_]: Async: Log](
      config: ValkeyClientConfig
  ): Resource[F, ValkeyClient[F]] = {
    val acquire: F[ValkeyClient[F]] = for {
      _ <- Log[F].info(
        s"Creating Valkey client for addresses: ${config.addresses.map(a => s"${a.host}:${a.port}").mkString(", ")}"
      )
      glideConfig = config.toGlide
      client <- Async[F].fromCompletableFuture(
        Async[F].delay(GlideClient.createClient(glideConfig))
      )
      _ <- Log[F].info("Valkey client created successfully")
    } yield ValkeyClientImpl[F](client)

    val release: ValkeyClient[F] => F[Unit] = client =>
      (for {
        _ <- Log[F].info("Closing Valkey client")
        _ <- Async[F].blocking(client.underlying.close())
        _ <- Log[F].info("Valkey client closed")
      } yield ()).handleErrorWith { err =>
        Log[F].error(s"Error closing Valkey client: ${err.getMessage}")
      }

    Resource.make(acquire)(release)
  }

  /** Convenience constructor from URI string
    *
    * Examples:
    * - "redis://localhost:6379"
    * - "rediss://my-secure-server:6380"
    * - "redis://:mypassword@localhost:6379/2"
    *
    * @param uri The connection URI
    * @return Resource managing the client
    */
  def fromUri[F[_]: Async: Log](uri: ValkeyUri): Resource[F, ValkeyClient[F]] =
    {
      val config = ValkeyClientConfig.fromUri(uri)
      fromConfig[F](config)
    }

  def fromUri[F[_]: Async: Log](uri: String): Resource[F, ValkeyClient[F]] =
  {
    Resource.eval(ValkeyClientConfig.fromUri(uri)).flatMap(fromConfig[F])
  }
  /** Convenience constructor for localhost with defaults */
  def localhost[F[_]: Async: Log]: Resource[F, ValkeyClient[F]] =
    fromConfig(ValkeyClientConfig.localhost)
}
