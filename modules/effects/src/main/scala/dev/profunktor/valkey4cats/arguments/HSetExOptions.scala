package dev.profunktor.valkey4cats.arguments

import glide.api.models.commands.{
  ExpirySet => GlideExpirySet,
  FieldConditionalChange
}

sealed trait ExpirySet { self =>
  private[valkey4cats] def toGlide: GlideExpirySet =
    self match {
      case ExpirySet.Seconds(s)           => GlideExpirySet.Seconds(s)
      case ExpirySet.Milliseconds(ms)     => GlideExpirySet.Milliseconds(ms)
      case ExpirySet.UnixSeconds(ts)      => GlideExpirySet.UnixSeconds(ts)
      case ExpirySet.UnixMilliseconds(ts) => GlideExpirySet.UnixMilliseconds(ts)
      case ExpirySet.Persist              => GlideExpirySet.Persist()
      case ExpirySet.KeepExisting         => GlideExpirySet.KeepExisting()
    }
}

object ExpirySet {
  case class Seconds(value: Long) extends ExpirySet
  case class Milliseconds(value: Long) extends ExpirySet
  case class UnixSeconds(value: Long) extends ExpirySet
  case class UnixMilliseconds(value: Long) extends ExpirySet
  case object Persist extends ExpirySet
  case object KeepExisting extends ExpirySet
}

sealed trait FieldCondition { self =>
  private[valkey4cats] def toGlide: FieldConditionalChange =
    self match {
      case FieldCondition.OnlyIfAllExist =>
        FieldConditionalChange.ONLY_IF_ALL_EXIST
      case FieldCondition.OnlyIfNoneExist =>
        FieldConditionalChange.ONLY_IF_NONE_EXIST
    }
}

object FieldCondition {
  case object OnlyIfAllExist extends FieldCondition
  case object OnlyIfNoneExist extends FieldCondition
}
