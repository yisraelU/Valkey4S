# Valkey4S Testing Guide

## Test Suite Overview

Valkey4S includes a comprehensive test suite covering:
- **Unit Tests**: Codec round-trips, configuration parsing
- **Integration Tests**: Real commands against Valkey/Redis via TestContainers

### Test Statistics

**Phase 1 Test Coverage:**
- ✅ 19 Unit Tests (all passing)
- ✅ 30+ Integration Tests
- **Total:** 49+ test cases

## Running Tests

### Prerequisites

For integration tests, you need:
1. **Docker** - Must be running for TestContainers
2. **Java 11+** - Required by Glide
3. **sbt** - Build tool

### Quick Start

```bash
# Run all tests (requires Docker)
sbt test

# Run only unit tests (no Docker required)
sbt "glideCore/testOnly io.github.yisraelu.valkey4s.codec.*"
sbt "glideCore/testOnly io.github.yisraelu.valkey4s.model.*"

# Run only integration tests (requires Docker)
sbt "glideEffects/testOnly io.github.yisraelu.valkey4s.StringCommandsSuite"
sbt "glideEffects/testOnly io.github.yisraelu.valkey4s.KeyCommandsSuite"
```

### Running Tests by Module

```bash
# Core module tests (unit tests only)
sbt glideCore/test

# Effects module tests (integration tests)
sbt glideEffects/test

# All tests
sbt test
```

## Test Organization

### Module: `glide-core`

**Unit Tests (no Docker required):**

#### `ValkeyCodecSuite.scala`
Tests codec round-trip conversions:
- ✅ String encoding/decoding (UTF-8)
- ✅ Byte array handling
- ✅ Numeric types (Long, Int, Double)
- ✅ Edge cases (empty strings, negative numbers, scientific notation)

**Test Count:** 10 tests

#### `ValkeyClientConfigSuite.scala`
Tests configuration and URI parsing:
- ✅ Simple URI parsing (redis://)
- ✅ TLS URI parsing (rediss://)
- ✅ Password authentication
- ✅ Username+password authentication
- ✅ Database selection
- ✅ Default port handling
- ✅ Invalid scheme rejection
- ✅ Fluent builder API
- ✅ Default configurations

**Test Count:** 9 tests

### Module: `glide-effects`

**Integration Tests (Docker required):**

#### `StringCommandsSuite.scala`
Tests string operations against real Valkey:
- ✅ GET (existing & non-existent keys)
- ✅ SET (simple, UTF-8, overwrite)
- ✅ MGET (multiple keys, mixed existence)
- ✅ MSET (bulk set operations)
- ✅ INCR/INCRBY (counter increments)
- ✅ DECR/DECRBY (counter decrements)
- ✅ APPEND (string concatenation)
- ✅ STRLEN (string length)
- ✅ Complex workflows

**Test Count:** 18 tests

#### `KeyCommandsSuite.scala`
Tests key management against real Valkey:
- ✅ DEL (single, multiple, non-existent)
- ✅ EXISTS (existing & non-existent)
- ✅ EXISTSMANY (counting keys)
- ✅ Mixed key operations
- ✅ Stress test (100 keys)
- ✅ Complex workflows

**Test Count:** 12 tests

## Test Infrastructure

### `ValkeyTestSuite`
Base class for integration tests that:
- Automatically starts TestContainers Valkey instance
- Provides `valkeyClient` fixture
- Handles cleanup after tests
- Includes logging

### `ValkeyContainer`
TestContainers wrapper that:
- Spins up Valkey/Redis in Docker
- Exposes connection details (URI, host, port)
- Auto-cleanup on shutdown

## Example Test Output

```
[32mio.github.yisraelu.valkey4s.codec.ValkeyCodecSuite:[0m
[32m  + [0m[32mStringCodec should encode and decode strings correctly[0m [90m0.003s[0m
[32m  + [0m[32mStringCodec should handle empty strings[0m [90m0.001s[0m
[32m  + [0m[32mStringCodec should handle UTF-8 characters[0m [90m0.0s[0m
...
[info] Passed: Total 19, Failed 0, Errors 0, Passed 19
[success] Total time: 3 s
```

## Setting Up Docker for Integration Tests

### macOS/Linux
```bash
# Start Docker Desktop or Docker daemon
docker ps  # Verify it's running

# Pull Valkey image (optional, will auto-pull)
docker pull valkey/valkey:latest
```

### Windows
```powershell
# Start Docker Desktop

# Verify
docker ps
```

## Running Integration Tests

Once Docker is running:

```bash
# Run all integration tests
sbt glideEffects/test

# Run specific test suite
sbt "glideEffects/testOnly io.github.yisraelu.valkey4s.StringCommandsSuite"

# Run single test
sbt 'glideEffects/testOnly *StringCommandsSuite -- --tests="GET should return None for non-existent key"'
```

### Expected Behavior

1. **TestContainers starts Valkey**:
   ```
   Creating container for image: valkey/valkey:latest
   Container valkey/valkey:latest started
   ```

2. **Tests run**:
   ```
   [32m  + [0m[32mGET should return None for non-existent key[0m
   [32m  + [0m[32mSET and GET should work for simple string[0m
   ...
   ```

3. **Container cleanup**:
   ```
   Stopped container: valkey/valkey:latest
   ```

## Troubleshooting

### Tests Fail with Docker Connection Error

**Error:**
```
Cannot connect to the Docker daemon at unix:///var/run/docker.sock
```

**Solution:**
- Ensure Docker is running: `docker ps`
- Start Docker Desktop if on Mac/Windows
- Check Docker daemon on Linux: `sudo systemctl start docker`

### Tests Timeout

**Error:**
```
Test timed out after 2 minutes
```

**Solution:**
- Slow Docker pull - Wait for Valkey image to download
- Increase timeout in test configuration
- Check Docker resource allocation (CPU/Memory)

### Port Already in Use

**Error:**
```
Address already in use: 6379
```

**Solution:**
- TestContainers uses random ports, shouldn't happen
- If it does, stop conflicting service: `docker stop $(docker ps -q --filter "expose=6379")`

### Tests Pass Locally but Fail in CI

**Common Issues:**
1. Docker not available in CI environment
2. Network restrictions
3. Insufficient resources

**Solutions:**
- Ensure CI has Docker support
- Use CI-specific test profiles
- Increase resource limits

## Writing New Tests

### Unit Test Example

```scala
import munit.FunSuite

class MyCodecSuite extends FunSuite {
  test("my codec should work") {
    val codec = ValkeyCodec.myCodec
    val original = MyType("test")

    val encoded = codec.encode(original)
    val decoded = codec.decode(encoded)

    assertEquals(decoded, original)
  }
}
```

### Integration Test Example

```scala
class MyCommandsSuite extends ValkeyTestSuite {
  test("my command should work") {
    valkeyClient.use { valkey =>
      for {
        _      <- valkey.myCommand("key", "value")
        result <- valkey.get("key")
        _      <- valkey.del("key") // Cleanup!
      } yield assertEquals(result, Some("value"))
    }
  }
}
```

### Best Practices

1. **Always cleanup**: Use `valkey.del()` after tests
2. **Use unique keys**: Avoid test interference
3. **Test edge cases**: Empty strings, special chars, boundaries
4. **Test errors**: Invalid inputs, non-existent keys
5. **Keep tests fast**: Use `valkeyClient` fixture, don't recreate

## Continuous Integration

### GitHub Actions Example

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      valkey:
        image: valkey/valkey:latest
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run tests
        run: sbt test
```

## Test Coverage Goals

### Phase 1 (Current)
- ✅ Core codecs
- ✅ Configuration parsing
- ✅ String commands
- ✅ Key commands

### Phase 2 (Planned)
- ⏳ Hash commands
- ⏳ List commands
- ⏳ Set commands
- ⏳ Sorted Set commands

### Phase 3 (Future)
- ⏳ Transaction tests
- ⏳ Pipeline tests
- ⏳ Pub/Sub tests
- ⏳ Cluster tests
- ⏳ Property-based tests (ScalaCheck)

## Performance Testing

### Benchmark Example

```scala
test("throughput benchmark: 1000 SETs") {
  valkeyClient.use { valkey =>
    val start = System.currentTimeMillis()

    for {
      _ <- (1 to 1000).toList.traverse { i =>
        valkey.set(s"bench-$i", s"value-$i")
      }
      _ <- valkey.del((1 to 1000).map(i => s"bench-$i"): _*)
    } yield {
      val elapsed = System.currentTimeMillis() - start
      println(s"1000 SETs took ${elapsed}ms")
      assert(elapsed < 5000) // Should be under 5 seconds
    }
  }
}
```

## Useful Commands

```bash
# Watch tests (re-run on file change)
sbt ~glideCore/test

# Run tests with verbose output
sbt 'set logLevel := Level.Debug' glideEffects/test

# Run tests and generate coverage report
sbt coverage test coverageReport

# Run only failed tests
sbt glideEffects/testQuick
```

## Test Files Location

```
modules/
├── glide-core/src/test/scala/
│   ├── codec/
│   │   └── ValkeyCodecSuite.scala
│   ├── model/
│   │   └── ValkeyClientConfigSuite.scala
│   └── util/
│       └── ValkeyContainer.scala
│
└── glide-effects/src/test/scala/
    ├── ValkeyTestSuite.scala
    ├── StringCommandsSuite.scala
    └── KeyCommandsSuite.scala
```

---

## Summary

✅ **19 unit tests** - All passing, no Docker required
✅ **30+ integration tests** - Require Docker, comprehensive coverage
✅ **Test infrastructure** - TestContainers, fixtures, utilities
✅ **Documentation** - This guide!

**To run all tests:**
```bash
# 1. Start Docker
docker ps

# 2. Run tests
sbt test
```

**Current Status:**
- Unit tests: ✅ All passing
- Integration tests: ⏳ Require Docker to be running

Happy testing! 🧪
