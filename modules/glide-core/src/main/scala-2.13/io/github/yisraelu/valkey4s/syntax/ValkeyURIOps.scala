package io.github.yisraelu.valkey4s.syntax

import io.github.yisraelu.valkey4s.model.ValkeyUri

class ValkeyURIOps(val sc: StringContext) extends AnyVal {
  def valkey(args: Any*): ValkeyUri = macro macros.ValkeyLiteral.make
}

trait ValkeySyntax {
  implicit def toValkeyURIOps(sc: StringContext): ValkeyURIOps =
    new ValkeyURIOps(sc)
}

object literals extends ValkeySyntax