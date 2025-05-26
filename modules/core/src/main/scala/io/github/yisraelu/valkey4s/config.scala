package io.github.yisraelu.valkey4s

import scala.concurrent.duration.*

object config {

  // Builder-style abstract class instead of case class to allow for bincompat-friendly extension in future versions.
  sealed abstract class Valkey4SConfig {
    val connectionTimeout: FiniteDuration

  }

  object Valkey4SConfig {
    private case class Valkey4SConfigImpl(
                                             shutdown: ShutdownConfig,
                                             topologyViewRefreshStrategy: TopologyViewRefreshStrategy = NoRefresh,
                                             nodeFilter: RedisClusterNode => Boolean = ClusterClientOptions.DEFAULT_NODE_FILTER.test,
                                             clientResources: Option[ClientResources] = None
                                           ) extends Valkey4SConfig {
      override def withShutdown(_shutdown: ShutdownConfig): Valkey4SConfig = copy(shutdown = _shutdown)
      override def withTopologyViewRefreshStrategy(strategy: TopologyViewRefreshStrategy): Valkey4SConfig =
        copy(topologyViewRefreshStrategy = strategy)
      override def withNodeFilter(_nodeFilter: RedisClusterNode => Boolean): Valkey4SConfig =
        copy(nodeFilter = _nodeFilter)
      override def withClientResources(resources: Option[ClientResources]): Valkey4SConfig =
        copy(clientResources = resources)
    }
    def apply(): Valkey4SConfig = Valkey4SConfigImpl(ShutdownConfig())
  }
}