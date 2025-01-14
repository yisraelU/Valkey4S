package dev.profunktor.valkey4cats.arguments

import glide.api.models.commands.bitmap.{
  BitmapIndexType => GlideBitmapIndexType,
  BitwiseOperation => GlideBitwiseOperation
}

sealed trait BitmapIndexType {
  def toGlide: GlideBitmapIndexType = this match {
    case BitmapIndexType.Byte => GlideBitmapIndexType.BYTE
    case BitmapIndexType.Bit  => GlideBitmapIndexType.BIT
  }
}

object BitmapIndexType {
  case object Byte extends BitmapIndexType
  case object Bit extends BitmapIndexType
}

sealed trait BitwiseOperation {
  def toGlide: GlideBitwiseOperation = this match {
    case BitwiseOperation.And => GlideBitwiseOperation.AND
    case BitwiseOperation.Or  => GlideBitwiseOperation.OR
    case BitwiseOperation.Xor => GlideBitwiseOperation.XOR
    case BitwiseOperation.Not => GlideBitwiseOperation.NOT
  }
}

object BitwiseOperation {
  case object And extends BitwiseOperation
  case object Or extends BitwiseOperation
  case object Xor extends BitwiseOperation
  case object Not extends BitwiseOperation
}
