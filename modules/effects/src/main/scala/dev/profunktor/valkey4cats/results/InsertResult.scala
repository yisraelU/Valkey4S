package dev.profunktor.valkey4cats.results

sealed trait InsertResult

object InsertResult {
  case class Inserted(newLength: Long) extends InsertResult
  case object PivotNotFound extends InsertResult
}
