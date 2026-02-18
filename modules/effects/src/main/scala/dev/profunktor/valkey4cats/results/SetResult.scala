package dev.profunktor.valkey4cats.results

sealed trait SetResult[+V]

object SetResult {
  case object Written extends SetResult[Nothing]
  case class Replaced[V](oldValue: V) extends SetResult[V]
  case object NotSet extends SetResult[Nothing]
}
