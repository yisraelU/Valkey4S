package io.github.yisraelu.valkey4s.effect

import cats.effect.*
import io.github.yisraelu.valkey4s.connection.{ValkeyClient, ValkeyClusterClient}
import io.github.yisraelu.valkey4s.model.{ValkeyClientConfig, ValkeyClusterConfig, ValkeyUri}

import scala.annotation.implicitNotFound

/** MkValkey is a capability trait that abstracts over the creation of ValkeyClient, ValkeyClusterClient,
  * and related infrastructure.
  *
  * It serves the internal purpose to orchestrate creation of such instances while avoiding impure constraints
  * such as `Async` or `Sync`.
  *
  * Users only need a `MkValkey` constraint and `Async` to create a `Valkey` instance.
  */
@implicitNotFound(
  "MkValkey instance not found. You can summon one by having instances for cats.effect.Async and io.github.yisraelu.valkey4s.effect.Log in scope"
)
sealed trait MkValkey[F[_]] {

  /** Create standalone client from URI */
  def clientFromUri(uri: ValkeyUri): Resource[F, ValkeyClient[F]]

  def clientFromUri(uri: String): Resource[F, ValkeyClient[F]]

  /** Create standalone client from config */
  def clientFromConfig(config: ValkeyClientConfig): Resource[F, ValkeyClient[F]]

  /** Create cluster client from config */
  def clusterClient(
      config: ValkeyClusterConfig
  ): Resource[F, ValkeyClusterClient[F]]

  /** Create cluster client from URI strings */
  def clusterClientFromUris(
      uris: String*
  ): Resource[F, ValkeyClusterClient[F]]

  /** Transaction runner (stub for Phase 1, full implementation in Phase 3) */
  private[valkey4s] def txRunner: Resource[F, TxRunner[F]]

  /** Access to FutureLift */
  private[valkey4s] def futureLift: FutureLift[F]

  /** Logging capability */
  private[valkey4s] def log: Log[F]
}

object MkValkey {
  def apply[F[_]: MkValkey]: MkValkey[F] = implicitly

  implicit def forAsync[F[_]: Async: Log]: MkValkey[F] = new MkValkey[F] {

    def clientFromUri(uri: String): Resource[F, ValkeyClient[F]] =
      ValkeyClient.fromUri[F](uri)

    def clientFromUri(uri: ValkeyUri): Resource[F, ValkeyClient[F]] = ValkeyClient.fromUri[F](uri)

    def clientFromConfig(
        config: ValkeyClientConfig
    ): Resource[F, ValkeyClient[F]] =
      ValkeyClient.fromConfig[F](config)

    def clusterClient(
        config: ValkeyClusterConfig
    ): Resource[F, ValkeyClusterClient[F]] =
      ValkeyClusterClient.fromConfig[F](config)

    def clusterClientFromUris(
        uris: String*
    ): Resource[F, ValkeyClusterClient[F]] =
      ValkeyClusterClient.fromUris[F](uris*)


    private[valkey4s] def txRunner: Resource[F, TxRunner[F]] =
      Resource.eval(TxRunner.make[F])

    private[valkey4s] def futureLift: FutureLift[F] = implicitly

    private[valkey4s] def log: Log[F] = implicitly
  }
}
