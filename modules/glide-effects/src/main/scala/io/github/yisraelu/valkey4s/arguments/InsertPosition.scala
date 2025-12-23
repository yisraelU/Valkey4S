package io.github.yisraelu.valkey4s.arguments

import glide.api.models.commands.LInsertOptions
import io.github.yisraelu.valkey4s.arguments.InsertPosition.{After, Before}

/** Position for LINSERT command */
sealed trait InsertPosition { self =>
  private[valkey4s] def toGlide: LInsertOptions.InsertPosition =
    self match {
      case Before => LInsertOptions.InsertPosition.BEFORE
      case After  => LInsertOptions.InsertPosition.AFTER
    }
}

object InsertPosition {
  /** Insert before the pivot element */
  case object Before extends InsertPosition

  /** Insert after the pivot element */
  case object After extends InsertPosition


}
