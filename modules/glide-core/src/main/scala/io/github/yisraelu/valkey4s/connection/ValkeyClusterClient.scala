package io.github.yisraelu.valkey4s.connection

import cats.effect._
import cats.syntax.all._
import glide.api.GlideClusterClient
import io.github.yisraelu.valkey4s.effect.Log
import io.github.yisraelu.valkey4s.model.ValkeyClusterConfig

/** Wrapper around Glide's cluster client with functional resource management
  *
  * @param underlying The underlying GlideClusterClient instance
  */
sealed abstract class ValkeyClusterClient[F[_]] private (
    val underlying: GlideClusterClient
)

object ValkeyClusterClient {

  /** Create a ValkeyClusterClient from configuration with Resource management
    *
    * @param config The cluster configuration
    * @return Resource that manages the client lifecycle
    */
  def fromConfig[F[_]: Async: Log](
      config: ValkeyClusterConfig
  ): Resource[F, ValkeyClusterClient[F]] = {
    val acquire: F[ValkeyClusterClient[F]] = for {
      _ <- Log[F].info(
        s"Creating Valkey cluster client for seed nodes: ${config.addresses.map(a => s"${a.host}:${a.port}").mkString(", ")}"
      )
      glideConfig = config.toGlide
      client <- Async[F].fromCompletableFuture(
        Async[F].delay(GlideClusterClient.createClient(glideConfig))
      )
      _ <- Log[F].info("Valkey cluster client created successfully")
    } yield new ValkeyClusterClient[F](client) {}

    val release: ValkeyClusterClient[F] => F[Unit] = client =>
      (for {
        _ <- Log[F].info("Closing Valkey cluster client")
        _ <- Async[F].blocking(client.underlying.close())
        _ <- Log[F].info("Valkey cluster client closed")
      } yield ()).handleErrorWith { err =>
        Log[F].error(s"Error closing Valkey cluster client: ${err.getMessage}")
      }

    Resource.make(acquire)(release)
  }

  /** Create from list of URI strings
    *
    * @param uris List of seed node URIs
    * @return Resource managing the cluster client
    */
  def fromUris[F[_]: Async: Log](
      uris: String*
  ): Resource[F, ValkeyClusterClient[F]] =
    Resource
      .eval(ValkeyClusterConfig.fromUris[F](uris.toList))
      .flatMap(fromConfig[F])
}
