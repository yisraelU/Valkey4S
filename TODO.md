# Valkey4S TODO

## Refactoring & Improvements

### Configuration API Completeness

- [x] **Add `reconnectStrategy` to ValkeyClusterConfig** ✅
  - Location: `modules/glide-core/src/main/scala/io/github/yisraelu/valkey4s/model/ValkeyClusterConfig.scala`
  - GlideClusterClientConfiguration supports `reconnectStrategy()`
  - Should match ValkeyClientConfig for consistency
  - Add field, update copy/apply/unapply, add `withReconnectStrategy()` builder method
  - Update `toGlide` to set `builder.reconnectStrategy()`

- [x] **Add `protocolVersion` to ValkeyClusterConfig** ✅
  - Location: `modules/glide-core/src/main/scala/io/github/yisraelu/valkey4s/model/ValkeyClusterConfig.scala`
  - GlideClusterClientConfiguration supports `protocol()`
  - Should match ValkeyClientConfig (defaults to RESP3)
  - Add field, update copy/apply/unapply
  - Update `toGlide` to set `builder.protocol()`

### API Consistency

- [x] **Refactor `NodeAddress.toGlide` to instance method** ✅
  - Location: `modules/glide-core/src/main/scala/io/github/yisraelu/valkey4s/model/NodeAddress.scala`
  - Current: `NodeAddress.toGlide(addr)` (companion object method)
  - Proposed: `addr.toGlide` (instance method)
  - More idiomatic, matches pattern of ServerCredentials, ReadFromStrategy, etc.
  - Update all call sites in ValkeyClientConfig and ValkeyClusterConfig

- [ ] **Rename `withAddress` to `addAddress` in ValkeyClusterConfig**
  - Location: `modules/glide-core/src/main/scala/io/github/yisraelu/valkey4s/model/ValkeyClusterConfig.scala:69-73`
  - Current name is misleading (it appends, doesn't replace)
  - ValkeyClientConfig has both `withAddress` (replace) and `addAddress` (append)
  - Cluster config should only have `addAddress` since it always appends

- [ ] **Add `withTlsEnabled` and `withTlsDisabled` to ValkeyClusterConfig**
  - Location: `modules/glide-core/src/main/scala/io/github/yisraelu/valkey4s/model/ValkeyClusterConfig.scala`
  - ValkeyClientConfig has these convenience methods
  - Currently only has `withTls(enabled: Boolean)`
  - Add for API consistency:
    ```scala
    def withTlsEnabled: ValkeyClusterConfig = copy(useTls = true)
    def withTlsDisabled: ValkeyClusterConfig = copy(useTls = false)
    ```

### Code Quality

- [ ] **Add ScalaDoc examples to advanced configuration methods**
  - Targets:
    - `withClientAZ` - show usage with AZ_AFFINITY
    - `withLazyConnect` - explain when to use
    - `withInflightRequestsLimit` - performance tuning context
    - `withConnectionTimeout` - timeout vs request timeout distinction
  - Follow existing pattern in codebase

- [ ] **Consider extracting client instantiation logic**
  - Locations:
    - `ValkeyClient.scala:36` - `new ValkeyClient[F](client) {}`
    - `ValkeyClusterClient.scala:36` - `new ValkeyClusterClient[F](client) {}`
  - Empty anonymous class bodies
  - Could add private factory methods in companion objects
  - Low priority - current approach works fine

### Future Phases (Phase 3)

- [ ] **Implement transaction support**
  - See: `modules/glide-core/src/main/scala/io/github/yisraelu/valkey4s/effect/TxRunner.scala:10`
  - Currently has TODO comment
  - Blocked until Phase 3

- [ ] **Add pub/sub configuration support**
  - GlideClientConfiguration has `subscriptionConfiguration`
  - Deliberately deferred to Phase 3 (per previous discussion)

## Notes

- All changes should maintain binary compatibility
- All new features need tests
- Follow pattern matching style (not inheritance/dynamic dispatch)
- Use sealed abstract classes with private implementations
- Import Glide with `import glide.api.models.{configuration => G}`
