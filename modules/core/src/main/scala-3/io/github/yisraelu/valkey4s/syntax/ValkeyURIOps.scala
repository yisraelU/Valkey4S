package dev.profunktor.valkey4cats.syntax

import dev.profunktor.valkey4cats.model.ValkeyUri

extension (inline ctx: StringContext) {
  inline def valkey(inline args: Any*): ValkeyUri =
    ${ macros.ValkeyLiteral.apply('ctx, 'args) }
}

object literals:
  export dev.profunktor.valkey4cats.syntax.valkey
