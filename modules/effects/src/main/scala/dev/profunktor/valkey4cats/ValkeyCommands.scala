package dev.profunktor.valkey4cats

import dev.profunktor.valkey4cats.algebra.*

/** Main command trait - extends all command algebras
  *
  * Phase 1: String and Key commands
  * Phase 2: Hash, List, Set, SortedSet commands (in progress)
  * Phase 3: Transactions, Pub/Sub (not yet implemented)
  */
trait ValkeyCommands[F[_], K, V]
    extends StringCommands[F, K, V]
    with KeyCommands[F, K, V]
    with HashCommands[F, K, V]
    with ListCommands[F, K, V]
    with SetCommands[F, K, V]
    with SortedSetCommands[F, K, V]
