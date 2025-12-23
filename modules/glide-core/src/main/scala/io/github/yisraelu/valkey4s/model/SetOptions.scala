package io.github.yisraelu.valkey4s.model

import glide.api.models.{commands => G}

/** Expiry options for SET command */
sealed trait SetExpiry {
  private[valkey4s] def toGlide: G.SetOptions.Expiry
}

object SetExpiry {
  /** Keep the existing TTL */
  case object KeepExisting extends SetExpiry {
    override private[valkey4s] def toGlide: G.SetOptions.Expiry =
      G.SetOptions.Expiry.KeepExisting()
  }

  /** Set expiry in seconds */
  final case class Seconds(seconds: Long) extends SetExpiry {
    override private[valkey4s] def toGlide: G.SetOptions.Expiry =
      G.SetOptions.Expiry.Seconds(seconds)
  }

  /** Set expiry in milliseconds */
  final case class Milliseconds(milliseconds: Long) extends SetExpiry {
    override private[valkey4s] def toGlide: G.SetOptions.Expiry =
      G.SetOptions.Expiry.Milliseconds(milliseconds)
  }

  /** Set expiry as Unix timestamp in seconds */
  final case class UnixSeconds(timestamp: Long) extends SetExpiry {
    override private[valkey4s] def toGlide: G.SetOptions.Expiry =
      G.SetOptions.Expiry.UnixSeconds(timestamp)
  }

  /** Set expiry as Unix timestamp in milliseconds */
  final case class UnixMilliseconds(timestamp: Long) extends SetExpiry {
    override private[valkey4s] def toGlide: G.SetOptions.Expiry =
      G.SetOptions.Expiry.UnixMilliseconds(timestamp)
  }
}

/** Conditional set options for SET command */
sealed trait SetCondition {
  private[valkey4s] def apply(builder: G.SetOptions.SetOptionsBuilder): G.SetOptions.SetOptionsBuilder
}

object SetCondition {
  /** Only set the key if it does not already exist (NX) */
  case object OnlyIfNotExists extends SetCondition {
    override private[valkey4s] def apply(builder: G.SetOptions.SetOptionsBuilder): G.SetOptions.SetOptionsBuilder =
      builder.conditionalSetOnlyIfNotExist()
  }

  /** Only set the key if it already exists (XX) */
  case object OnlyIfExists extends SetCondition {
    override private[valkey4s] def apply(builder: G.SetOptions.SetOptionsBuilder): G.SetOptions.SetOptionsBuilder =
      builder.conditionalSetOnlyIfExists()
  }

  /** Only set if the current value equals the given value */
  final case class OnlyIfEqualTo(value: String) extends SetCondition {
    override private[valkey4s] def apply(builder: G.SetOptions.SetOptionsBuilder): G.SetOptions.SetOptionsBuilder =
      builder.conditionalSetOnlyIfEqualTo(value)
  }
}

/** Options for SET command
  *
  * Example:
  * {{{
  * import io.github.yisraelu.valkey4s.model._
  *
  * // Set with 60 second expiry
  * SetOptions().withExpiry(SetExpiry.Seconds(60))
  *
  * // Set only if not exists with expiry
  * SetOptions()
  *   .withCondition(SetCondition.OnlyIfNotExists)
  *   .withExpiry(SetExpiry.Seconds(60))
  *
  * // Set and return old value
  * SetOptions().withReturnOldValue
  * }}}
  */
final case class SetOptions(
    expiry: Option[SetExpiry] = None,
    condition: Option[SetCondition] = None,
    returnOldValue: Boolean = false
) {

  /** Set expiry time */
  def withExpiry(exp: SetExpiry): SetOptions =
    copy(expiry = Some(exp))

  /** Set conditional behavior */
  def withCondition(cond: SetCondition): SetOptions =
    copy(condition = Some(cond))

  /** Return the old value */
  def withReturnOldValue: SetOptions =
    copy(returnOldValue = true)

  /** Convert to Glide's SetOptions */
  private[valkey4s] def toGlide: G.SetOptions = {
    var builder = G.SetOptions.builder()

    expiry.foreach(e => builder = builder.expiry(e.toGlide))
    condition.foreach(c => builder = c.apply(builder))
    if (returnOldValue) {
      builder = builder.returnOldValue(true)
    }

    builder.build()
  }
}

object SetOptions {
  /** Create empty SetOptions */
  def apply(): SetOptions = new SetOptions()
}
