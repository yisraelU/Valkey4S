package io.github.yisraelu.valkey4s.model

import glide.api.models.commands.LInsertOptions

/** Position for LINSERT command */
sealed trait InsertPosition {
  private[valkey4s] def toGlide: LInsertOptions.InsertPosition
}

object InsertPosition {
  /** Insert before the pivot element */
  case object Before extends InsertPosition {
    override private[valkey4s] def toGlide: LInsertOptions.InsertPosition =
      LInsertOptions.InsertPosition.BEFORE
  }

  /** Insert after the pivot element */
  case object After extends InsertPosition {
    override private[valkey4s] def toGlide: LInsertOptions.InsertPosition =
      LInsertOptions.InsertPosition.AFTER
  }
}
