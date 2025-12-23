package io.github.yisraelu.valkey4s.model

import glide.api.models.{commands => G}

/** Conditional change mode for ZADD command */
sealed trait ZAddConditionalChange {
  private[valkey4s] def toGlide: G.ZAddOptions.ConditionalChange
}

object ZAddConditionalChange {
  /** Only add new elements. Don't update existing elements (NX) */
  case object OnlyIfNotExists extends ZAddConditionalChange {
    override private[valkey4s] def toGlide: G.ZAddOptions.ConditionalChange =
      G.ZAddOptions.ConditionalChange.ONLY_IF_DOES_NOT_EXIST
  }

  /** Only update existing elements. Don't add new elements (XX) */
  case object OnlyIfExists extends ZAddConditionalChange {
    override private[valkey4s] def toGlide: G.ZAddOptions.ConditionalChange =
      G.ZAddOptions.ConditionalChange.ONLY_IF_EXISTS
  }
}

/** Update options for ZADD command */
sealed trait ZAddUpdateOption {
  private[valkey4s] def toGlide: G.ZAddOptions.UpdateOptions
}

object ZAddUpdateOption {
  /** Only update if new score is greater than current score (GT) */
  case object GreaterThan extends ZAddUpdateOption {
    override private[valkey4s] def toGlide: G.ZAddOptions.UpdateOptions =
      G.ZAddOptions.UpdateOptions.SCORE_GREATER_THAN_CURRENT
  }

  /** Only update if new score is less than current score (LT) */
  case object LessThan extends ZAddUpdateOption {
    override private[valkey4s] def toGlide: G.ZAddOptions.UpdateOptions =
      G.ZAddOptions.UpdateOptions.SCORE_LESS_THAN_CURRENT
  }
}

/** Options for ZADD command
  *
  * Example:
  * {{{
  * import io.github.yisraelu.valkey4s.model._
  *
  * // Add only if not exists
  * ZAddOptions().withConditionalChange(ZAddConditionalChange.OnlyIfNotExists)
  *
  * // Update only if new score is greater
  * ZAddOptions().withUpdateOption(ZAddUpdateOption.GreaterThan)
  *
  * // Combine options (both NX/XX and GT/LT cannot be used together)
  * ZAddOptions()
  *   .withConditionalChange(ZAddConditionalChange.OnlyIfExists)
  *   .withUpdateOption(ZAddUpdateOption.GreaterThan)
  * }}}
  */
final case class ZAddOptions(
    conditionalChange: Option[ZAddConditionalChange] = None,
    updateOption: Option[ZAddUpdateOption] = None
) {

  /** Set conditional change mode (NX or XX) */
  def withConditionalChange(change: ZAddConditionalChange): ZAddOptions =
    copy(conditionalChange = Some(change))

  /** Set update option (GT or LT) */
  def withUpdateOption(option: ZAddUpdateOption): ZAddOptions =
    copy(updateOption = Some(option))

  /** Convert to Glide's ZAddOptions */
  private[valkey4s] def toGlide: G.ZAddOptions = {
    val builder = G.ZAddOptions.builder()

    conditionalChange.foreach(cc => builder.conditionalChange(cc.toGlide))
    updateOption.foreach(uo => builder.updateOptions(uo.toGlide))

    builder.build()
  }
}

object ZAddOptions {
  /** Create empty ZAddOptions */
  def apply(): ZAddOptions = new ZAddOptions()
}
