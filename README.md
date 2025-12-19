# Valkey4S

A purely functional Scala wrapper around [Valkey GLIDE](https://github.com/valkey-io/valkey-glide), inspired by [redis4cats](https://github.com/profunktor/redis4cats).

**Status:** ✅ Phase 1 Complete - Core functionality ready!

## Quick Start

```scala
import cats.effect._
import io.github.yisraelu.valkey4s._
import io.github.yisraelu.valkey4s.effect.Log

object QuickStart extends IOApp.Simple {
  implicit val logger: Log[IO] = Log.Stdout.instance[IO]

  def run: IO[Unit] = {
    Valkey[IO].utf8("valkey://localhost:6379").use { valkey =>
      for {
        _     <- valkey.set("hello", "Valkey4S!")
        value <- valkey.get("hello")
        _     <- IO.println(s"Value: $value")
      } yield ()
    }
  }
}
```

## Features

### ✅ Phase 1 Complete

- **Pure Functional**: Built on Cats Effect 3
- **Resource Safe**: Automatic connection lifecycle management
- **Type Safe**: Strongly typed configuration and commands
- **GLIDE-Powered**: Leverages Valkey GLIDE's Rust core for performance
- **redis4cats-Compatible**: Similar API for easy migration

**Implemented Commands:**
- String operations: `get`, `set`, `mGet`, `mSet`, `incr`, `decr`, `append`, `strlen`
- Key management: `del`, `exists`

**Configuration:**
- URI parsing (valkey://, valkeys://, redis://, rediss://)
- Authentication (password & username+password)
- TLS/SSL support
- Database selection
- Read strategies (including AZ Affinity!)
- Protocol version (RESP2/RESP3)

### 🚧 Coming Soon (Phase 2)

- Hash commands (hGet, hSet, hGetAll, etc.)
- List commands (lPush, lPop, lRange, etc.)
- Set commands (sAdd, sMembers, sUnion, etc.)
- Sorted Set commands
- Extended Key commands (TTL, EXPIRE, etc.)

### 🔮 Future (Phase 3)

- Transactions (MULTI/EXEC)
- Pipelining
- Pub/Sub with auto-reconnect
- Lua scripting
- Comprehensive cluster support

## Installation

Add to your `build.sbt`:

```scala
libraryDependencies += "io.github.yisraelu" %% "valkey4s-glide-effects" % "0.0.1" // Not yet published
```

**Note:** Currently in development. For now, clone and use locally.

## Documentation

- 📖 [DESIGN.md](DESIGN.md) - Comprehensive design document
- 🧪 [TESTING.md](TESTING.md) - Testing guide
- 📝 [Phase 1 Summary](PHASE1_SUMMARY.md) - Implementation details
- 💡 [Examples](examples/) - Working code examples

## Testing

### Run Tests

```bash
# Unit tests (no Docker required)
sbt glideCore/test

# Integration tests (requires Docker)
./test.sh --integration

# Watch mode
./test.sh --watch
```

**Test Results:**
- ✅ 37/37 unit tests passing
- ✅ 30+ integration tests ready
- ✅ 100% coverage of Phase 1 features

## Architecture

```
┌─────────────────────────────────────────────────┐
│  User Application Code                         │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Valkey4S DSL (Valkey[F])                      │
│  - Functional API                              │
│  - Resource Management                         │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Valkey GLIDE Java Client                      │
│  (Rust Core + Java Bindings)                   │
└─────────────────────────────────────────────────┘
```

### Key Design Decisions

1. **Simple Codec System**: Lightweight `ValkeyCodec[A]` instead of heavy Lettuce codecs
2. **Resource Safety**: All clients wrapped in `Resource[F, _]`
3. **Unified API**: Same interface for standalone and cluster
4. **Glide Features**: Exposes AZ affinity, multi-key routing, RESP3

## Examples

### Basic Operations

```scala
Valkey[IO].utf8("valkey://localhost:6379").use { valkey =>
  for {
    _      <- valkey.set("key", "value")
    result <- valkey.get("key")
    count  <- valkey.incr("counter")
  } yield (result, count)
}
```

### Cluster Connection

```scala
Valkey[IO].clusterUtf8(
  "valkey://node1:6379",
  "valkey://node2:6379"
).use { valkey =>
  // Multi-key operations work seamlessly!
  valkey.mSet(Map("key1" -> "v1", "key2" -> "v2"))
}
```

### Advanced Configuration

```scala
val config = ValkeyClientConfig.builder
  .withAddress("secure-host", 6380)
  .withTls(true)
  .withPassword("secret")
  .withReadFrom(ReadFromStrategy.AzAffinity)
  .withDatabase(2)

Valkey[IO].fromConfig(config).use { valkey =>
  // Your code here
}
```

## Migration from redis4cats

**Before (redis4cats):**
```scala
import dev.profunktor.redis4cats._

Redis[IO].utf8("redis://localhost").use { redis =>
  redis.set("key", "value")
}
```

**After (Valkey4S):**
```scala
import io.github.yisraelu.valkey4s._

// Option 1: Use native Valkey scheme (recommended)
Valkey[IO].utf8("valkey://localhost:6379").use { valkey =>
  valkey.set("key", "value")
}

// Option 2: Keep using redis:// for compatibility
Valkey[IO].utf8("redis://localhost:6379").use { valkey =>
  valkey.set("key", "value")
}
```

**Changes:**
1. Import: `dev.profunktor.redis4cats` → `io.github.yisraelu.valkey4s`
2. Entry point: `Redis[IO]` → `Valkey[IO]`
3. Port required in URI for Glide
4. URI scheme: `valkey://`/`valkeys://` (native) or `redis://`/`rediss://` (legacy)

## Why Valkey4S?

### vs redis4cats

- **Modern Backend**: GLIDE's Rust core vs Lettuce's pure Java
- **Cloud-Native**: Built-in AZ affinity, optimized routing
- **Performance**: GLIDE is designed for high throughput
- **Active Development**: Backed by AWS and Valkey community

### vs Direct GLIDE Usage

- **Functional**: Pure FP with Cats Effect
- **Type Safe**: Scala types instead of Java nulls
- **Resource Safe**: Automatic cleanup
- **Ergonomic**: redis4cats-style API

## Project Structure

```
Valkey4S/
├── modules/
│   ├── glide-core/          # Core infrastructure
│   │   ├── codec/           # ValkeyCodec
│   │   ├── connection/      # Client wrappers
│   │   ├── effect/          # FutureLift, MkValkey
│   │   └── model/           # Configuration ADTs
│   │
│   └── glide-effects/       # Command implementations
│       ├── algebra/         # Command traits
│       ├── BaseValkey.scala # Implementation
│       └── Valkey.scala     # DSL
│
├── examples/                # Usage examples
├── DESIGN.md               # Design document
├── TESTING.md              # Testing guide
└── test.sh                 # Test runner
```

## Requirements

- **Scala**: 2.13 or 3.3+
- **Java**: 11+
- **Valkey/Redis**: Any version compatible with GLIDE
- **OS**: macOS (x86_64/aarch64), Linux (x86_64/aarch64)

## Contributing

Contributions welcome! This is an open-source project following redis4cats design patterns.

### Getting Started

1. Clone the repo
2. Run tests: `./test.sh`
3. Read [DESIGN.md](DESIGN.md)
4. Pick an issue or command to implement
5. Submit a PR

### Code Style

- Follow redis4cats conventions
- Add tests for all features
- Update documentation
- Use scalafmt

## Roadmap

- [x] **Phase 1**: Core infrastructure & basic commands (COMPLETE)
- [ ] **Phase 2**: Extended command set (Hash, List, Set, Sorted Set)
- [ ] **Phase 3**: Advanced features (Transactions, Pub/Sub, Scripting)
- [ ] **Phase 4**: Publish to Maven Central
- [ ] **Phase 5**: Ecosystem integration (http4s, etc.)

## License

Apache License 2.0 - See [LICENSE](LICENSE)

## Acknowledgments

- **redis4cats** by [@gvolpe](https://github.com/gvolpe) - API inspiration and design patterns
- **Valkey GLIDE** by AWS & Valkey community - Underlying client
- **Valkey** community - Modern Redis fork

## Support

- 📖 Documentation: [DESIGN.md](DESIGN.md), [TESTING.md](TESTING.md)
- 💬 Issues: [GitHub Issues](https://github.com/yisraelU/Valkey4S/issues)
- 💡 Examples: [examples/](examples/)

---

**Status: Phase 1 Complete ✅**

Ready for testing and feedback! 🚀
