# Valkey4S TODO

## Refactoring & Improvements

### Configuration API Completeness



### API Consistency
  - missing  refreshTopologyFromInitialNodes from advancedcluster config
  - ignore pubsub for now

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
