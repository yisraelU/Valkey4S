package io.github.yisraelu.valkey4s.model

import glide.api.models.commands.{RangeOptions => G}

/** Range query for sorted set commands */
sealed trait RangeQuery {
  private[valkey4s] def toGlide: G.RangeQuery
}

object RangeQuery {
  /** Range by index (rank)
    *
    * @param start Start index (inclusive, 0-based, can be negative)
    * @param stop Stop index (inclusive, can be negative)
    */
  final case class ByIndex(start: Long, stop: Long) extends RangeQuery {
    override private[valkey4s] def toGlide: G.RangeQuery =
      new G.RangeByIndex(start, stop)
  }

  /** Range by score
    *
    * @param min Minimum score boundary
    * @param max Maximum score boundary
    */
  final case class ByScore(min: ScoreBoundary, max: ScoreBoundary) extends RangeQuery {
    override private[valkey4s] def toGlide: G.RangeQuery =
      new G.RangeByScore(min.toGlide, max.toGlide)
  }

  /** Range by lexicographic order
    *
    * @param min Minimum lex boundary
    * @param max Maximum lex boundary
    */
  final case class ByLex(min: LexBoundary, max: LexBoundary) extends RangeQuery {
    override private[valkey4s] def toGlide: G.RangeQuery =
      new G.RangeByLex(min.toGlide, max.toGlide)
  }
}

/** Score boundary for range queries */
sealed trait ScoreBoundary {
  private[valkey4s] def toGlide: G.ScoreRange
}

object ScoreBoundary {
  /** Positive infinity */
  case object PositiveInfinity extends ScoreBoundary {
    override private[valkey4s] def toGlide: G.ScoreRange =
      G.InfScoreBound.POSITIVE_INFINITY
  }

  /** Negative infinity */
  case object NegativeInfinity extends ScoreBoundary {
    override private[valkey4s] def toGlide: G.ScoreRange =
      G.InfScoreBound.NEGATIVE_INFINITY
  }

  /** Specific score value
    *
    * @param score The score value
    * @param inclusive Whether the boundary is inclusive (default: true)
    */
  final case class Score(score: Double, inclusive: Boolean = true) extends ScoreBoundary {
    override private[valkey4s] def toGlide: G.ScoreRange =
      new G.ScoreBoundary(score, inclusive)
  }
}

/** Lexicographic boundary for range queries */
sealed trait LexBoundary {
  private[valkey4s] def toGlide: G.LexRange
}

object LexBoundary {
  /** Positive infinity */
  case object PositiveInfinity extends LexBoundary {
    override private[valkey4s] def toGlide: G.LexRange =
      G.InfLexBound.POSITIVE_INFINITY
  }

  /** Negative infinity */
  case object NegativeInfinity extends LexBoundary {
    override private[valkey4s] def toGlide: G.LexRange =
      G.InfLexBound.NEGATIVE_INFINITY
  }

  /** Specific lexicographic value
    *
    * @param value The string value
    * @param inclusive Whether the boundary is inclusive (default: true)
    */
  final case class Lex(value: String, inclusive: Boolean = true) extends LexBoundary {
    override private[valkey4s] def toGlide: G.LexRange =
      new G.LexBoundary(value, inclusive)
  }
}

/** Limit for range queries */
final case class RangeLimit(offset: Long, count: Long) {
  private[valkey4s] def toGlide: G.Limit =
    new G.Limit(offset, count)
}

/** Options for ZRANGE and related commands */
final case class ZRangeOptions(
    reverse: Boolean = false,
    limit: Option[RangeLimit] = None
) {
  /** Return results in reverse order */
  def withReverse: ZRangeOptions =
    copy(reverse = true)

  /** Add limit to the range */
  def withLimit(offset: Long, count: Long): ZRangeOptions =
    copy(limit = Some(RangeLimit(offset, count)))
}

object ZRangeOptions {
  /** Create empty ZRangeOptions */
  def apply(): ZRangeOptions = new ZRangeOptions()
}
