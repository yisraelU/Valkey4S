package dev.profunktor.valkey4cats.arguments

import glide.api.models.commands.stream.{
  StreamRange => GlideStreamRange,
  StreamTrimOptions => GlideStreamTrimOptions
}

sealed trait StreamTrimStrategy {
  def toGlide: GlideStreamTrimOptions
}

object StreamTrimStrategy {
  final case class MaxLen(threshold: Long, exact: Boolean = true)
      extends StreamTrimStrategy {
    def toGlide: GlideStreamTrimOptions =
      new GlideStreamTrimOptions.MaxLen(exact, threshold)
  }

  final case class MinId(id: String, exact: Boolean = true)
      extends StreamTrimStrategy {
    def toGlide: GlideStreamTrimOptions =
      new GlideStreamTrimOptions.MinId(exact, id)
  }
}

sealed trait StreamRangeBound {
  def toGlide: GlideStreamRange
}

object StreamRangeBound {
  case object Min extends StreamRangeBound {
    def toGlide: GlideStreamRange = GlideStreamRange.InfRangeBound.MIN
  }

  case object Max extends StreamRangeBound {
    def toGlide: GlideStreamRange = GlideStreamRange.InfRangeBound.MAX
  }

  final case class Id(id: String) extends StreamRangeBound {
    def toGlide: GlideStreamRange = GlideStreamRange.IdBound.of(id)
  }

  final case class ExclusiveId(id: String) extends StreamRangeBound {
    def toGlide: GlideStreamRange =
      GlideStreamRange.IdBound.ofExclusive(id)
  }
}
