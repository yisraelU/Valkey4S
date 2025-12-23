package dev.profunktor.valkey4cats.syntax

import dev.profunktor.valkey4cats.model.ValkeyUri

class ValkeyURIOps(val sc: StringContext) extends AnyVal {
  def valkey(args: Any*): ValkeyUri = macro macros.ValkeyLiteral.make
}

trait ValkeySyntax {
  implicit def toValkeyURIOps(sc: StringContext): ValkeyURIOps =
    new ValkeyURIOps(sc)
}

object literals extends ValkeySyntax
