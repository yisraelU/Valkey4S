package dev.profunktor.valkey4cats.arguments

import glide.api.models.commands.{ScoreFilter => GlideScoreFilter}

sealed trait ScoreFilter {
  private[valkey4cats] def toGlide: GlideScoreFilter
}

object ScoreFilter {
  case object Min extends ScoreFilter {
    def toGlide: GlideScoreFilter = GlideScoreFilter.MIN
  }
  case object Max extends ScoreFilter {
    def toGlide: GlideScoreFilter = GlideScoreFilter.MAX
  }
}
