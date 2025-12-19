# Valkey4S Design Document

## Executive Summary

Valkey4S is a purely functional Scala wrapper around the Valkey GLIDE Java client, inspired by redis4cats. The goal is to provide an idiomatic, type-safe Scala API while leveraging Glide's superior performance, reliability features, and modern cloud-native capabilities.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Design Philosophy](#design-philosophy)
3. [Type Mappings](#type-mappings)
4. [Module Structure](#module-structure)
5. [Core Components](#core-components)
6. [API Examples](#api-examples)
7. [Migration from redis4cats](#migration-from-redis4cats)
8. [Implementation Roadmap](#implementation-roadmap)

---

## Architecture Overview

### Layers

```
┌─────────────────────────────────────────────────┐
│  User Application Code                         │
│  (Uses Valkey4S functional API)                │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Valkey4S High-Level API                       │
│  - Valkey[F] DSL                               │
│  - Command Algebra Traits                      │
│  - Resource Management                         │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Valkey4S Core Layer                           │
│  - FutureLift (CompletableFuture → F[_])      │
│  - Client Wrappers                             │
│  - Connection Management                       │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Valkey GLIDE Java Client                      │
│  (Rust core + Java bindings)                   │
└─────────────────────────────────────────────────┘
```

### Key Architectural Differences: redis4cats vs Valkey4S

| Aspect | redis4cats | Valkey4S |
|--------|-----------|----------|
| **Underlying Client** | Lettuce (pure Java, Netty-based) | Glide (Rust core with Java bindings) |
| **Connection Model** | StatefulConnection wrapping | Direct client usage |
| **Async Primitive** | Lettuce RedisFuture | Java CompletableFuture |
| **String Type** | String / ByteBuffer | GlideString (unified byte[] wrapper) |
| **Codec System** | Lettuce RedisCodec | Minimal conversion layer |
| **Cluster Routing** | Manual key grouping | Automatic multi-key routing |
| **Special Features** | Standard | AZ affinity, auto-reconnect PubSub |

---

## Design Philosophy

### 1. **Functional First**
- All operations in `F[_]` with `Async`, `MonadThrow` constraints
- Resource-safe connection management using `Resource[F, _]`
- No mutable state exposed to users

### 2. **Keep redis4cats Spirit**
- Similar API surface for easier migration
- Same algebra traits (StringCommands, HashCommands, etc.)
- Familiar patterns: `Valkey[F].simple()`, `.utf8()`, etc.

### 3. **Leverage Glide's Strengths**
- Expose Glide-specific features (AZ affinity, sharded pub/sub)
- Use Glide's optimized cluster operations
- Minimal abstraction overhead

### 4. **Type Safety**
- Phantom types for key/value codecs where beneficial
- Clear separation between standalone and cluster operations
- Compile-time guarantees for configuration

---

## Type Mappings

### Core Type Conversions

#### Glide → Scala

```scala
// Glide Java Types
glide.api.models.GlideString              → String | Array[Byte]
java.util.concurrent.CompletableFuture[T] → F[T]
glide.api.models.configuration.*          → Valkey4S config ADTs

// Client Types
glide.api.GlideClient                     → ValkeyClient[F]
glide.api.GlideClusterClient              → ValkeyClusterClient[F]

// Configuration
glide.api.models.configuration.NodeAddress                 → NodeAddress
glide.api.models.configuration.GlideClientConfiguration    → ValkeyClientConfig
glide.api.models.configuration.GlideClusterClientConfiguration → ValkeyClusterConfig
```

#### Codec Strategy

Unlike Lettuce's heavy RedisCodec system, Glide uses GlideString for all operations. Our approach:

```scala
// Simple type class for encoding/decoding
trait ValkeyCodec[A] {
  def encode(value: A): GlideString
  def decode(gs: GlideString): A
}

object ValkeyCodec {
  // Built-in instances
  implicit val stringCodec: ValkeyCodec[String] = ???
  implicit val byteArrayCodec: ValkeyCodec[Array[Byte]] = ???

  // Derivation for case classes (future enhancement)
  implicit def jsonCodec[A: Encoder: Decoder]: ValkeyCodec[A] = ???
}
```

**Key Decision**: Keep it simple initially. Most users will use `String` or `Array[Byte]`. Advanced codecs can be added later.

---

## Module Structure

### Current Modules

```
valkey4s/
├── modules/
│   ├── core/                    # Core types and effects
│   │   ├── connection/          # Client wrappers
│   │   ├── effect/              # FutureLift, MkValkey, etc.
│   │   ├── model/               # Configuration ADTs
│   │   └── codec/               # (NEW) Codec type classes
│   │
│   └── effects/                 # Command implementations
│       ├── algebra/             # Command trait definitions
│       ├── commands.scala       # ValkeyCommands trait
│       └── valkey.scala         # Main Valkey[F] DSL
│
└── examples/                    # (NEW) Usage examples
    ├── standalone/
    └── cluster/
```

---

## Core Components

### 1. Client Wrappers

#### ValkeyClient (Standalone)

```scala
// modules/core/src/main/scala/io/github/yisraelu/valkey4s/connection/ValkeyClient.scala

package io.github.yisraelu.valkey4s.connection

import glide.api.GlideClient
import cats.effect._

/** Wrapper around Glide's standalone client with resource management */
sealed abstract class ValkeyClient[F[_]] private (
  val underlying: GlideClient
) {
  def close: F[Unit]
}

object ValkeyClient {

  /** Create a client from configuration */
  def fromConfig[F[_]: Async](config: ValkeyClientConfig): Resource[F, ValkeyClient[F]] =
    Resource.make(
      acquire = Async[F].fromCompletableFuture(
        Async[F].delay(GlideClient.createClient(config.toGlideConfig))
      ).map(client => new ValkeyClient[F](client) {
        def close: F[Unit] = Async[F].fromCompletableFuture(
          Async[F].delay(client.close())
        ).void
      })
    )(release = _.close)

  /** Convenience constructor from URI string */
  def simple[F[_]: Async](uri: String): Resource[F, ValkeyClient[F]] =
    Resource.eval(ValkeyClientConfig.fromUri[F](uri))
      .flatMap(fromConfig[F])
}
```

#### ValkeyClusterClient

```scala
// Similar structure but wraps GlideClusterClient

sealed abstract class ValkeyClusterClient[F[_]] private (
  val underlying: GlideClusterClient
) {
  def close: F[Unit]
}

object ValkeyClusterClient {
  def fromConfig[F[_]: Async](config: ValkeyClusterConfig): Resource[F, ValkeyClusterClient[F]] = ???

  def simple[F[_]: Async](uris: String*): Resource[F, ValkeyClusterClient[F]] = ???
}
```

### 2. Configuration ADTs

```scala
// modules/core/src/main/scala/io/github/yisraelu/valkey4s/model/config.scala

package io.github.yisraelu.valkey4s.model

import scala.concurrent.duration.FiniteDuration

/** Node address (host:port) */
final case class NodeAddress(
  host: String,
  port: Int
)

object NodeAddress {
  def apply(host: String): NodeAddress = NodeAddress(host, 6379)
}

/** Standalone client configuration */
final case class ValkeyClientConfig(
  addresses: List[NodeAddress],
  useTls: Boolean = false,
  requestTimeout: Option[FiniteDuration] = None,
  credentials: Option[ServerCredentials] = None,
  readFrom: Option[ReadFromStrategy] = None,
  reconnectStrategy: Option[BackOffStrategy] = None,
  databaseId: Option[Int] = None,
  clientName: Option[String] = None,
  protocolVersion: ProtocolVersion = ProtocolVersion.RESP3
) {
  def toGlideConfig: glide.api.models.configuration.GlideClientConfiguration = {
    // Builder pattern to convert to Glide Java config
    val builder = glide.api.models.configuration.GlideClientConfiguration.builder()

    addresses.foreach { addr =>
      builder.address(
        glide.api.models.configuration.NodeAddress.builder()
          .host(addr.host)
          .port(addr.port)
          .build()
      )
    }

    builder.useTLS(useTls)
    requestTimeout.foreach(d => builder.requestTimeout(d.toMillis.toInt))
    credentials.foreach(c => builder.credentials(c.toGlideCredentials))
    readFrom.foreach(r => builder.readFrom(r.toGlideReadFrom))
    reconnectStrategy.foreach(s => builder.reconnectStrategy(s.toGlideBackoff))
    databaseId.foreach(builder.databaseId(_))
    clientName.foreach(builder.clientName(_))

    builder.build()
  }
}

object ValkeyClientConfig {
  /** Parse from Redis URI string (redis://host:port or rediss://host:port) */
  def fromUri[F[_]: ApplicativeThrow](uri: String): F[ValkeyClientConfig] =
    ApplicativeThrow[F].catchNonFatal {
      // Parse URI manually (Glide doesn't have URI parsing)
      val useTls = uri.startsWith("rediss://")
      val withoutScheme = uri.replaceFirst("^rediss?://", "")

      val (hostPort, dbPart) = withoutScheme.split("/", 2) match {
        case Array(hp, db) => (hp, Some(db.toInt))
        case Array(hp)     => (hp, None)
      }

      val Array(host, portStr) = hostPort.split(":", 2)
      val port = if (portStr.nonEmpty) portStr.toInt else 6379

      ValkeyClientConfig(
        addresses = List(NodeAddress(host, port)),
        useTls = useTls,
        databaseId = dbPart
      )
    }
}

/** Cluster client configuration */
final case class ValkeyClusterConfig(
  addresses: List[NodeAddress],
  useTls: Boolean = false,
  requestTimeout: Option[FiniteDuration] = None,
  credentials: Option[ServerCredentials] = None,
  readFrom: Option[ReadFromStrategy] = None,
  clientName: Option[String] = None
) {
  def toGlideConfig: glide.api.models.configuration.GlideClusterClientConfiguration = ???
}
```

### 3. MkValkey Typeclass

```scala
// modules/core/src/main/scala/io/github/yisraelu/valkey4s/effect/MkValkey.scala

package io.github.yisraelu.valkey4s.effect

import cats.effect._
import io.github.yisraelu.valkey4s.connection._
import io.github.yisraelu.valkey4s.model._

/** Capability trait for creating Valkey clients and related infrastructure */
trait MkValkey[F[_]] {
  /** Create standalone client from URI */
  def clientFromUri(uri: String): Resource[F, ValkeyClient[F]]

  /** Create standalone client from config */
  def clientFromConfig(config: ValkeyClientConfig): Resource[F, ValkeyClient[F]]

  /** Create cluster client */
  def clusterClient(config: ValkeyClusterConfig): Resource[F, ValkeyClusterClient[F]]

  /** Transaction runner (for MULTI/EXEC support) */
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
      ValkeyClient.simple[F](uri)

    def clientFromConfig(config: ValkeyClientConfig): Resource[F, ValkeyClient[F]] =
      ValkeyClient.fromConfig[F](config)

    def clusterClient(config: ValkeyClusterConfig): Resource[F, ValkeyClusterClient[F]] =
      ValkeyClusterClient.fromConfig[F](config)

    def txRunner: Resource[F, TxRunner[F]] =
      TxExecutor.make[F].map(TxRunner.make[F])

    def futureLift: FutureLift[F] = implicitly

    def log: Log[F] = implicitly
  }
}
```

### 4. Command Implementation

```scala
// modules/effects/src/main/scala/io/github/yisraelu/valkey4s/commands.scala

package io.github.yisraelu.valkey4s

import cats.effect._
import io.github.yisraelu.valkey4s.effect._
import io.github.yisraelu.valkey4s.algebra._

/** Main command trait - extends all command algebras */
trait ValkeyCommands[F[_], K, V]
  extends StringCommands[F, K, V]
  with HashCommands[F, K, V]
  with ListCommands[F, K, V]
  with SetCommands[F, K, V]
  with SortedSetCommands[F, K, V]
  with KeyCommands[F, K, V]
  with ServerCommands[F, K, V]
  with ScriptingCommands[F, K, V]
  with TransactionCommands[F, K, V]

/** Base implementation for both standalone and cluster */
private[valkey4s] abstract class BaseValkey[F[_]: Async: Log, K, V](
  protected val client: Either[ValkeyClient[F], ValkeyClusterClient[F]],
  protected val keyCodec: ValkeyCodec[K],
  protected val valueCodec: ValkeyCodec[V],
  protected val tx: TxRunner[F]
) extends ValkeyCommands[F, K, V] {

  import FutureLift.FutureLiftOps

  /** Get the underlying Glide client (works for both standalone and cluster) */
  private def glideClient: BaseClient = client match {
    case Left(standalone) => standalone.underlying
    case Right(cluster) => cluster.underlying
  }

  // String commands implementation
  override def get(key: K): F[Option[V]] = {
    val keyGS = keyCodec.encode(key)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(glideClient.get(keyGS))
      )
      .map(Option(_).map(valueCodec.decode))
  }

  override def set(key: K, value: V): F[Unit] = {
    val keyGS = keyCodec.encode(key)
    val valueGS = valueCodec.encode(value)
    Async[F]
      .fromCompletableFuture(
        Async[F].delay(glideClient.set(keyGS, valueGS))
      )
      .void
  }

  // ... more command implementations
}

/** Standalone client commands */
private[valkey4s] class ValkeyStandalone[F[_]: Async: Log, K, V](
  client: ValkeyClient[F],
  keyCodec: ValkeyCodec[K],
  valueCodec: ValkeyCodec[V],
  tx: TxRunner[F]
) extends BaseValkey[F, K, V](Left(client), keyCodec, valueCodec, tx)

/** Cluster client commands */
private[valkey4s] class ValkeyCluster[F[_]: Async: Log, K, V](
  client: ValkeyClusterClient[F],
  keyCodec: ValkeyCodec[K],
  valueCodec: ValkeyCodec[V],
  tx: TxRunner[F]
) extends BaseValkey[F, K, V](Right(client), keyCodec, valueCodec, tx)
```

### 5. High-Level DSL

```scala
// modules/effects/src/main/scala/io/github/yisraelu/valkey4s/valkey.scala

package io.github.yisraelu.valkey4s

import cats.effect._
import io.github.yisraelu.valkey4s.effect._
import io.github.yisraelu.valkey4s.model._

object Valkey {

  class ValkeyPartiallyApplied[F[_]: MkValkey: Async: Log] {

    /** Simple UTF-8 string connection */
    def utf8(uri: String): Resource[F, ValkeyCommands[F, String, String]] =
      for {
        client <- MkValkey[F].clientFromUri(uri)
        tx     <- MkValkey[F].txRunner
      } yield new ValkeyStandalone[F, String, String](
        client,
        ValkeyCodec.stringCodec,
        ValkeyCodec.stringCodec,
        tx
      )

    /** Custom codec connection */
    def simple[K, V](
      uri: String
    )(implicit
      kCodec: ValkeyCodec[K],
      vCodec: ValkeyCodec[V]
    ): Resource[F, ValkeyCommands[F, K, V]] =
      for {
        client <- MkValkey[F].clientFromUri(uri)
        tx     <- MkValkey[F].txRunner
      } yield new ValkeyStandalone[F, K, V](client, kCodec, vCodec, tx)

    /** From existing client */
    def fromClient[K, V](
      client: ValkeyClient[F]
    )(implicit
      kCodec: ValkeyCodec[K],
      vCodec: ValkeyCodec[V]
    ): Resource[F, ValkeyCommands[F, K, V]] =
      MkValkey[F].txRunner.map { tx =>
        new ValkeyStandalone[F, K, V](client, kCodec, vCodec, tx)
      }

    /** Cluster connection */
    def clusterUtf8(
      seedUris: String*
    ): Resource[F, ValkeyCommands[F, String, String]] =
      for {
        config <- Resource.eval(ValkeyClusterConfig.fromUris[F](seedUris.toList))
        client <- MkValkey[F].clusterClient(config)
        tx     <- MkValkey[F].txRunner
      } yield new ValkeyCluster[F, String, String](
        client,
        ValkeyCodec.stringCodec,
        ValkeyCodec.stringCodec,
        tx
      )
  }

  def apply[F[_]: MkValkey: Async: Log]: ValkeyPartiallyApplied[F] =
    new ValkeyPartiallyApplied[F]
}
```

---

## API Examples

### Example 1: Basic Usage (Standalone)

```scala
import cats.effect._
import io.github.yisraelu.valkey4s._
import io.github.yisraelu.valkey4s.effect.Log.Stdout._

object BasicExample extends IOApp.Simple {
  def run: IO[Unit] = {
    Valkey[IO].utf8("redis://localhost:6379").use { valkey =>
      for {
        _     <- valkey.set("mykey", "Hello, Valkey!")
        value <- valkey.get("mykey")
        _     <- IO.println(s"Value: $value")

        // Hash operations
        _ <- valkey.hSet("user:1", Map(
          "name" -> "Alice",
          "age" -> "30",
          "city" -> "NYC"
        ))
        user <- valkey.hGetAll("user:1")
        _ <- IO.println(s"User: $user")

      } yield ()
    }
  }
}
```

### Example 2: Cluster with Custom Configuration

```scala
import cats.effect._
import io.github.yisraelu.valkey4s._
import io.github.yisraelu.valkey4s.model._
import scala.concurrent.duration._

object ClusterExample extends IOApp.Simple {
  def run: IO[Unit] = {
    val config = ValkeyClusterConfig(
      addresses = List(
        NodeAddress("node1.cluster.local", 6379),
        NodeAddress("node2.cluster.local", 6379),
        NodeAddress("node3.cluster.local", 6379)
      ),
      requestTimeout = Some(2.seconds),
      readFrom = Some(ReadFromStrategy.PreferReplica),
      useTls = true,
      credentials = Some(ServerCredentials.password("my-secret"))
    )

    MkValkey[IO].clusterClient(config).flatMap { client =>
      Valkey[IO].fromClient(client)
    }.use { valkey =>
      for {
        // Multi-key operations work automatically across slots!
        _ <- valkey.mSet(Map(
          "key1" -> "value1",
          "key2" -> "value2",
          "key3" -> "value3"
        ))

        values <- valkey.mGet(Set("key1", "key2", "key3"))
        _ <- IO.println(s"Values: $values")

      } yield ()
    }
  }
}
```

### Example 3: Transactions

```scala
import cats.effect._
import io.github.yisraelu.valkey4s._

object TransactionExample extends IOApp.Simple {
  def run: IO[Unit] = {
    Valkey[IO].utf8("redis://localhost:6379").use { valkey =>

      // Transactional increment
      valkey.transact_ {
        List(
          valkey.incr("counter"),
          valkey.incr("counter"),
          valkey.set("last-update", System.currentTimeMillis().toString)
        )
      }
    }
  }
}
```

### Example 4: Custom Codecs (JSON)

```scala
import cats.effect._
import io.github.yisraelu.valkey4s._
import io.github.yisraelu.valkey4s.codec._
import io.circe._
import io.circe.syntax._
import io.circe.parser._

case class User(id: String, name: String, email: String)

object User {
  implicit val codec: Codec[User] = ???

  implicit val valkeyCodec: ValkeyCodec[User] = new ValkeyCodec[User] {
    def encode(user: User): GlideString =
      GlideString.of(user.asJson.noSpaces.getBytes("UTF-8"))

    def decode(gs: GlideString): User =
      decode[User](new String(gs.getBytes, "UTF-8")).fold(throw _, identity)
  }
}

object JsonExample extends IOApp.Simple {
  def run: IO[Unit] = {
    Valkey[IO].simple[String, User]("redis://localhost:6379").use { valkey =>
      val user = User("123", "Alice", "alice@example.com")

      for {
        _ <- valkey.set("user:123", user)
        retrieved <- valkey.get("user:123")
        _ <- IO.println(s"Retrieved: $retrieved")
      } yield ()
    }
  }
}
```

### Example 5: Leveraging Glide Features (AZ Affinity)

```scala
import cats.effect._
import io.github.yisraelu.valkey4s._
import io.github.yisraelu.valkey4s.model._

object AzAffinityExample extends IOApp.Simple {
  def run: IO[Unit] = {
    val config = ValkeyClusterConfig(
      addresses = List(
        NodeAddress("cluster.us-east-1a.aws", 6379)
      ),
      // Prefer reading from replicas in the same AZ for lower latency
      readFrom = Some(ReadFromStrategy.AzAffinity)
    )

    MkValkey[IO].clusterClient(config).flatMap { client =>
      Valkey[IO].fromClient(client)
    }.use { valkey =>
      // Reads will be routed to local replicas when possible
      valkey.get("frequently-read-key")
    }
  }
}
```

---

## Migration from redis4cats

### Side-by-Side Comparison

#### redis4cats

```scala
import dev.profunktor.redis4cats._
import dev.profunktor.redis4cats.effect.Log.Stdout._

Redis[IO].utf8("redis://localhost").use { redis =>
  for {
    _ <- redis.set("key", "value")
    v <- redis.get("key")
  } yield v
}
```

#### Valkey4S

```scala
import io.github.yisraelu.valkey4s._
import io.github.yisraelu.valkey4s.effect.Log.Stdout._

Valkey[IO].utf8("redis://localhost").use { valkey =>
  for {
    _ <- valkey.set("key", "value")
    v <- valkey.get("key")
  } yield v
}
```

**Changes needed:**
1. Import path: `dev.profunktor.redis4cats` → `io.github.yisraelu.valkey4s`
2. Entry point: `Redis[IO]` → `Valkey[IO]`
3. Variable naming convention: `redis` → `valkey` (stylistic)

### Breaking Changes

1. **No Lettuce Types Exposed**
   - redis4cats: Could access `RedisClient`, `RedisURI`, etc.
   - Valkey4S: Only Valkey4S types exposed

2. **Different Connection Model**
   - redis4cats: `StatefulConnection` concept
   - Valkey4S: Direct client wrapper

3. **Codec System Simpler**
   - redis4cats: `RedisCodec[K, V]` (Lettuce)
   - Valkey4S: `ValkeyCodec[A]` (simpler, one type at a time)

4. **Some Commands Renamed/Removed**
   - Commands not supported by Glide will need alternative approaches
   - Glide has some commands redis4cats doesn't (e.g., sharded pub/sub)

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1)
**Goal:** Working standalone client with basic string operations

- [x] Fix build.sbt (Glide dependency with classifier)
- [x] FutureLift (already using CompletableFuture)
- [ ] ValkeyCodec trait + String/ByteArray instances
- [ ] NodeAddress, ServerCredentials models
- [ ] ValkeyClientConfig ADT + fromUri parser
- [ ] ValkeyClient wrapper with Resource management
- [ ] MkValkey typeclass
- [ ] BaseValkey skeleton
- [ ] String commands: get, set, mGet, mSet, incr, decr
- [ ] Basic integration test

**Deliverable:** Can connect to Valkey and do basic KV operations

### Phase 2: Core Commands (Week 2)
**Goal:** Feature parity with common redis4cats operations

- [ ] Complete String commands (SetArgs, GetEx, etc.)
- [ ] Hash commands (hGet, hSet, hGetAll, etc.)
- [ ] List commands (lPush, rPop, lRange, etc.)
- [ ] Set commands (sAdd, sMembers, sInter, etc.)
- [ ] Key commands (del, exists, expire, ttl, etc.)
- [ ] ValkeyClusterClient wrapper
- [ ] ValkeyClusterConfig
- [ ] Cluster-aware command routing

**Deliverable:** Can use Valkey4S for typical application needs

### Phase 3: Advanced Features (Week 3)
**Goal:** Full feature set including unique Glide capabilities

- [ ] Sorted Set commands
- [ ] Geo commands
- [ ] Bitmap commands
- [ ] HyperLogLog commands
- [ ] Transaction support (MULTI/EXEC)
- [ ] Pipeline support
- [ ] Lua scripting (EVAL/EVALSHA)
- [ ] Pub/Sub with auto-reconnect
- [ ] Sharded Pub/Sub (Glide-specific)
- [ ] AZ Affinity configuration
- [ ] Comprehensive test suite
- [ ] Documentation site

**Deliverable:** Production-ready library

### Phase 4: Ecosystem (Ongoing)
- [ ] Benchmarks vs redis4cats
- [ ] Migration guide
- [ ] Example applications
- [ ] Integration with popular libraries (http4s, etc.)
- [ ] Publish to Maven Central
- [ ] Community building

---

## Open Questions & Design Decisions

### 1. Should we support both String and GlideString?

**Option A:** Always use GlideString internally, convert at boundaries
```scala
def get(key: String): F[Option[String]] // User-facing
// Internally: GlideString.of(key.getBytes) → call Glide → decode back
```

**Option B:** Expose GlideString to advanced users
```scala
def get(key: GlideString): F[Option[GlideString]]  // Power users
def getString(key: String): F[Option[String]]      // Convenience
```

**Recommendation:** Option A. Keep it simple. Power users can use `ValkeyCodec[Array[Byte]]`.

### 2. How to handle cluster-only commands?

**Option A:** Separate traits
```scala
trait ClusterCommands[F[_]] {
  def clusterInfo: F[String]
  def clusterNodes: F[List[NodeInfo]]
}

// Only available on cluster client
class ValkeyCluster extends ValkeyCommands with ClusterCommands
```

**Option B:** Fail at runtime
```scala
// All clients have these methods, but standalone throws UnsupportedOperation
def clusterInfo: F[String]
```

**Recommendation:** Option A. Compile-time safety is valuable.

### 3. Should transactions use a different API?

redis4cats uses `TxStore` for collecting results:

```scala
redis.transact { store =>
  List(
    store.set("a", "1"),
    store.get("b"),  // Can reference result later via store
  )
}
```

**Question:** Is this worth the complexity, or is a simpler API better?

```scala
// Simpler but can't access intermediate results
valkey.transact_(List(
  valkey.set("a", "1"),
  valkey.incr("counter")
))
```

**Recommendation:** Start simple. Add TxStore later if needed.

### 4. How to expose Glide-specific features?

Features like:
- AZ affinity routing
- Sharded pub/sub
- Specific timeout per request

**Option A:** Configuration-only
```scala
ValkeyClusterConfig(readFrom = Some(ReadFromStrategy.AzAffinity))
```

**Option B:** Runtime control
```scala
valkey.withReadFrom(ReadFromStrategy.AzAffinity).get("key")
```

**Recommendation:** Start with Option A (config). Add Option B if demand exists.

---

## Testing Strategy

### Unit Tests
- Codec round-trips
- Configuration parsing
- Error handling

### Integration Tests
- TestContainers with Valkey
- All command implementations
- Cluster scenarios

### Property-Based Tests
- ScalaCheck for codec properties
- Transaction isolation properties

### Performance Tests
- Throughput benchmarks vs redis4cats
- Latency percentiles
- Cluster routing efficiency

---

## Documentation Plan

1. **README.md**: Quick start, features, installation
2. **DESIGN.md**: This document
3. **MIGRATION.md**: redis4cats → Valkey4S guide
4. **API Docs**: Scaladoc for all public APIs
5. **Examples**: Real-world usage patterns
6. **Website** (mdoc): https://valkey4s.github.io

---

## Success Metrics

1. **API Compatibility**: 80%+ redis4cats APIs work with minimal changes
2. **Performance**: Match or exceed redis4cats throughput
3. **Reliability**: No resource leaks, proper error handling
4. **Adoption**: 5+ production users within 6 months
5. **Community**: Active contributor base

---

## Conclusion

Valkey4S aims to bring the functional programming ergonomics of redis4cats to the modern, high-performance world of Valkey GLIDE. By carefully balancing compatibility with innovation, we can create a library that's both familiar to Scala developers and takes full advantage of Valkey's capabilities.

The phased roadmap ensures we build a solid foundation before tackling advanced features, while the modular design allows for future extensibility without breaking changes.

**Next Steps:**
1. Review and approve this design
2. Begin Phase 1 implementation
3. Set up CI/CD pipeline
4. Create initial test suite structure
