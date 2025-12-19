package io.github.yisraelu.valkey4s

import io.github.yisraelu.valkey4s.algebra._

/** Main command trait - extends all command algebras
  *
  * For Phase 1, we only include String and Key commands.
  * More command groups will be added in Phase 2 and 3.
  */
trait ValkeyCommands[F[_], K, V]
    extends StringCommands[F, K, V]
    with KeyCommands[F, K, V]
