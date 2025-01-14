package dev.profunktor.valkey4cats.arguments

import glide.api.models.commands.WeightAggregateOptions.{
  Aggregate => GlideAggregate
}

sealed trait AggregateOption {
  private[valkey4cats] def toGlide: GlideAggregate
}

object AggregateOption {
  case object Sum extends AggregateOption {
    def toGlide: GlideAggregate = GlideAggregate.SUM
  }
  case object Min extends AggregateOption {
    def toGlide: GlideAggregate = GlideAggregate.MIN
  }
  case object Max extends AggregateOption {
    def toGlide: GlideAggregate = GlideAggregate.MAX
  }
}
