package dev.profunktor.valkey4cats.syntax

import dev.profunktor.valkey4cats.model.ValkeyUri
import org.typelevel.literally.Literally

object macros {

  object ValkeyLiteral extends Literally[ValkeyUri] {

    override def validate(
        c: Context
    )(s: String): Either[String, c.Expr[ValkeyUri]] = {
      import c.universe._
      ValkeyUri.fromString(s) match {
        case Left(e) => Left(e.getMessage)
        case Right(_) =>
          Right(
            c.Expr(
              q"_root_.dev.profunktor.valkey4cats.model.ValkeyUri.unsafeFromString($s)"
            )
          )
      }
    }

    def make(c: Context)(args: c.Expr[Any]*): c.Expr[ValkeyUri] =
      apply(c)(args: _*)
  }

}
