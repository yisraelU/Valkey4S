# Valkey4S Glide Core

This module contains the core infrastructure for Valkey4S, built on top of Valkey GLIDE Java client.

## Overview

The `glide-core` module provides:

- **Client Wrappers**: `ValkeyClient` and `ValkeyClusterClient` with Resource management
- **Configuration Models**: Type-safe ADTs for all client configuration options
- **Codec System**: Simple type class for encoding/decoding values
- **Effect Integration**: `FutureLift` for CompletableFuture → F[_] conversion
- **Infrastructure**: `MkValkey` typeclass for client creation

## Key Components

### Client Wrappers

```scala
import io.github.yisraelu.valkey4s.connection._

// Create a standalone client
ValkeyClient.fromUri[IO]("redis://localhost:6379")

// Create a cluster client
ValkeyClusterClient.fromUris[IO](
  "redis://node1:6379",
  "redis://node2:6379",
  "redis://node3:6379"
)
```

### Configuration

```scala
import io.github.yisraelu.valkey4s.model._
import scala.concurrent.duration._

val config = ValkeyClientConfig(
  addresses = List(NodeAddress("localhost", 6379)),
  useTls = false,
  requestTimeout = Some(2.seconds),
  credentials = Some(ServerCredentials.password("secret")),
  readFrom = Some(ReadFromStrategy.PreferReplica),
  protocolVersion = ProtocolVersion.RESP3
)
```

### Codecs

```scala
import io.github.yisraelu.valkey4s.codec._

// Built-in codecs
ValkeyCodec.utf8Codec // UTF-8 strings
ValkeyCodec.byteArrayCodec // Raw bytes
ValkeyCodec.longCodec // Longs
ValkeyCodec.intCodec // Ints
ValkeyCodec.doubleCodec // Doubles

// Custom codec
implicit val myCodec: ValkeyCodec[MyType] = new ValkeyCodec[MyType] {
  def encode(value: MyType): GlideString = ???

  def decode(gs: GlideString): MyType = ???
}
```

## Dependencies

This module depends on:
- Valkey GLIDE 2.2.3 (with platform-specific classifier)
- Cats Core 2.13.0
- Cats Effect 3.6.3

The platform-specific classifier is automatically selected based on your OS and architecture.
