# Valkey4S Glide Effects

This module contains the command implementations and high-level DSL for Valkey4S.

## Overview

The `glide-effects` module provides:

- **Command Algebras**: Trait definitions for all Valkey commands
- **Command Implementations**: Actual implementations using Glide client
- **High-Level DSL**: `Valkey[F]` entry point for easy usage

## Usage

### Basic Example

```scala
import cats.effect._
import io.github.yisraelu.valkey4s._
import io.github.yisraelu.valkey4s.effect.Log

object MyApp extends IOApp.Simple {
  implicit val logger: Log[IO] = Log.Stdout.instance[IO]

  def run: IO[Unit] = {
    Valkey[IO].utf8("redis://localhost:6379").use { valkey =>
      for {
        _     <- valkey.set("key", "value")
        value <- valkey.get("key")
        _     <- IO.println(s"Value: $value")
      } yield ()
    }
  }
}
```

### Available Commands (Phase 1)

#### String Commands
- `get(key: K): F[Option[V]]`
- `set(key: K, value: V): F[Unit]`
- `mGet(keys: Set[K]): F[Map[K, V]]`
- `mSet(keyValues: Map[K, V]): F[Unit]`
- `incr(key: K): F[Long]`
- `incrBy(key: K, amount: Long): F[Long]`
- `decr(key: K): F[Long]`
- `decrBy(key: K, amount: Long): F[Long]`
- `append(key: K, value: V): F[Long]`
- `strlen(key: K): F[Long]`

#### Key Commands
- `del(keys: K*): F[Long]`
- `exists(key: K): F[Boolean]`
- `existsMany(keys: K*): F[Long]`

More command groups (Hash, List, Set, etc.) will be added in Phase 2.

## Entry Points

### Standalone Connection

```scala
// Simple UTF-8 connection
Valkey[IO].utf8("redis://localhost:6379")

// Custom codecs
Valkey[IO].simple[String, MyType]("redis://localhost:6379")

// From configuration
Valkey[IO].fromConfig(config)

// Localhost shortcut
Valkey[IO].localhost
```

### Cluster Connection

```scala
// Simple UTF-8 cluster
Valkey[IO].clusterUtf8(
  "redis://node1:6379",
  "redis://node2:6379"
)

// Custom codecs
Valkey[IO].cluster[String, MyType]("redis://...")

// From configuration
Valkey[IO].fromClusterConfig(clusterConfig)
```

## Migration from redis4cats

Replace:
```scala
import dev.profunktor.redis4cats._
Redis[IO].utf8("redis://localhost").use { redis =>
  // ...
}
```

With:
```scala
import io.github.yisraelu.valkey4s._
Valkey[IO].utf8("redis://localhost:6379").use { valkey =>
  // ...
}
```

The API is intentionally similar for easy migration!
