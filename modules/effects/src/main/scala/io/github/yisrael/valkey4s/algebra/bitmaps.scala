

package io.github.yisrael.valkey4s.algebra

import dev.profunktor.redis4cats.algebra.BitCommandOperation.Overflows.Overflows
import io.lettuce.core.BitFieldArgs.BitFieldType

sealed trait BitCommandOperation

object BitCommandOperation {
  final case class Get(bitFieldType: BitFieldType, offset: Int) extends BitCommandOperation

  final case class SetSigned(offset: Int, value: Long, bits: Int = 1) extends BitCommandOperation

  final case class SetUnsigned(offset: Int, value: Long, bits: Int = 1) extends BitCommandOperation

  final case class IncrSignedBy(offset: Int, increment: Long, bits: Int = 1) extends BitCommandOperation

  final case class IncrUnsignedBy(offset: Int, increment: Long, bits: Int = 1) extends BitCommandOperation

  final case class Overflow(overflow: Overflows) extends BitCommandOperation

  object Overflows extends Enumeration {
    type Overflows = Value
    val WRAP, SAT, FAIL = Value
  }
}

trait BitCommands[F[_], K, V] {
  def bitCount(key: K): F[Long]

  def bitCount(key: K, start: Long, end: Long): F[Long]

  def bitField(key: K, operations: BitCommandOperation*): F[List[Long]]

  def bitOpAnd(destination: K, source: K, sources: K*): F[Unit]

  def bitOpNot(destination: K, source: K): F[Unit]

  def bitOpOr(destination: K, source: K, sources: K*): F[Unit]

  def bitOpXor(destination: K, source: K, sources: K*): F[Unit]

  def bitPos(key: K, state: Boolean): F[Long]

  def bitPos(key: K, state: Boolean, start: Long): F[Long]

  def bitPos(key: K, state: Boolean, start: Long, end: Long): F[Long]

  def getBit(key: K, offset: Long): F[Option[Long]]

  def setBit(key: K, offset: Long, value: Int): F[Long]
}
