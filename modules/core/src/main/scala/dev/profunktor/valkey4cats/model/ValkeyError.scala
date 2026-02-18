package dev.profunktor.valkey4cats.model

sealed trait ValkeyError {
  def message: String
}

object ValkeyError {
  case class WrongType(message: String) extends ValkeyError
  case class CommandError(message: String) extends ValkeyError
  case class ReadOnly(message: String) extends ValkeyError
  case class CrossSlot(message: String) extends ValkeyError
  case class OutOfMemory(message: String) extends ValkeyError
  case class AuthError(message: String) extends ValkeyError
  case class NoScript(message: String) extends ValkeyError
  case class Busy(message: String) extends ValkeyError
  case class TransactionAborted(message: String) extends ValkeyError
  case class Unexpected(message: String, cause: Option[Throwable] = None)
      extends ValkeyError

  def fromMessage(msg: String): ValkeyError = {
    val m = if (msg == null) "" else msg
    m match {
      case s if s.startsWith("WRONGTYPE") => WrongType(s)
      case s if s.startsWith("READONLY")  => ReadOnly(s)
      case s if s.startsWith("CROSSSLOT") => CrossSlot(s)
      case s if s.startsWith("OOM")       => OutOfMemory(s)
      case s if s.startsWith("NOAUTH")    => AuthError(s)
      case s if s.startsWith("WRONGPASS") => AuthError(s)
      case s if s.startsWith("NOSCRIPT")  => NoScript(s)
      case s if s.startsWith("BUSY")      => Busy(s)
      case s if s.startsWith("ERR")       => CommandError(s)
      case s                              => Unexpected(s)
    }
  }
}
