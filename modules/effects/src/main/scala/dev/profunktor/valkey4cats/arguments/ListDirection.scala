package dev.profunktor.valkey4cats.arguments

import glide.api.models.commands.{ListDirection => GlideListDirection}

sealed trait ListDirection {
  def toGlide: GlideListDirection = this match {
    case ListDirection.Left  => GlideListDirection.LEFT
    case ListDirection.Right => GlideListDirection.RIGHT
  }
}

object ListDirection {
  case object Left extends ListDirection
  case object Right extends ListDirection
}
