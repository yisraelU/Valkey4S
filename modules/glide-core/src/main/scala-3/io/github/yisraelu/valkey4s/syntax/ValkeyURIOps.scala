package io.github.yisraelu.valkey4s.syntax

import io.github.yisraelu.valkey4s.model.ValkeyUri

extension (inline ctx: StringContext) {
  inline def valkey(inline args: Any*): ValkeyUri =
    ${ macros.ValkeyLiteral.apply('ctx, 'args) }
}

object literals:
  export io.github.yisraelu.valkey4s.syntax.valkey
