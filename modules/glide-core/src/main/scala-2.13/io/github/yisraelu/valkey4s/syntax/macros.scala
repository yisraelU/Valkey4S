package io.github.yisraelu.valkey4s.syntax

import io.github.yisraelu.valkey4s.model.ValkeyUri
import org.typelevel.literally.Literally

object macros {

  object ValkeyLiteral extends Literally[ValkeyUri] {

    override def validate(c: Context)(s: String): Either[String, c.Expr[ValkeyUri]] = {
      import c.universe._
      ValkeyUri.fromString(s) match {
        case Left(e)  => Left(e.getMessage)
        case Right(_) => Right(c.Expr(q"_root_.io.github.yisraelu.valkey4s.model.ValkeyUri.unsafeFromString($s)"))
      }
    }

    def make(c: Context)(args: c.Expr[Any]*): c.Expr[ValkeyUri] = apply(c)(args: _*)
  }

}