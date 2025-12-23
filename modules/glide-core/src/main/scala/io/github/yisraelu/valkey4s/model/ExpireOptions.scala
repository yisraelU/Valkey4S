package io.github.yisraelu.valkey4s.model

import glide.api.models.commands.ExpireOptions

/** Conditional mode for EXPIRE command */
sealed trait ExpireCondition {
  private[valkey4s] def toGlide: ExpireOptions
}

object ExpireCondition {
  /** Set expiry only when the key has no expiry (NX) */
  case object OnlyIfNoExpiry extends ExpireCondition {
    override private[valkey4s] def toGlide: ExpireOptions =
      ExpireOptions.HAS_NO_EXPIRY
  }

  /** Set expiry only when the key has an existing expiry (XX) */
  case object OnlyIfHasExpiry extends ExpireCondition {
    override private[valkey4s] def toGlide: ExpireOptions =
      ExpireOptions.HAS_EXISTING_EXPIRY
  }

  /** Set expiry only when the new expiry is greater than current (GT) */
  case object OnlyIfGreater extends ExpireCondition {
    override private[valkey4s] def toGlide: ExpireOptions =
      ExpireOptions.NEW_EXPIRY_GREATER_THAN_CURRENT
  }

  /** Set expiry only when the new expiry is less than current (LT) */
  case object OnlyIfLess extends ExpireCondition {
    override private[valkey4s] def toGlide: ExpireOptions =
      ExpireOptions.NEW_EXPIRY_LESS_THAN_CURRENT
  }
}
