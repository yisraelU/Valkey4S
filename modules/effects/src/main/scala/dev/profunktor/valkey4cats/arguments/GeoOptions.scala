package dev.profunktor.valkey4cats.arguments

import glide.api.models.commands.geospatial.{
  GeoUnit => GlideGeoUnit,
  GeoAddOptions => GlideGeoAddOptions,
  GeoSearchOrigin => GlideGeoSearchOrigin,
  GeoSearchShape => GlideGeoSearchShape,
  GeoSearchResultOptions => GlideGeoSearchResultOptions,
  GeospatialData => GlideGeospatialData
}
import glide.api.models.commands.{
  ConditionalChange,
  SortOrder => GlideSortOrder
}

sealed trait GeoUnit {
  def toGlide: GlideGeoUnit = this match {
    case GeoUnit.Meters     => GlideGeoUnit.METERS
    case GeoUnit.Kilometers => GlideGeoUnit.KILOMETERS
    case GeoUnit.Miles      => GlideGeoUnit.MILES
    case GeoUnit.Feet       => GlideGeoUnit.FEET
  }
}

object GeoUnit {
  case object Meters extends GeoUnit
  case object Kilometers extends GeoUnit
  case object Miles extends GeoUnit
  case object Feet extends GeoUnit
}

case class GeoPosition(longitude: Double, latitude: Double) {
  def toGlide: GlideGeospatialData =
    new GlideGeospatialData(longitude, latitude)
}

sealed trait GeoAddCondition {
  def toGlide: ConditionalChange = this match {
    case GeoAddCondition.OnlyIfExists => ConditionalChange.ONLY_IF_EXISTS
    case GeoAddCondition.OnlyIfDoesNotExist =>
      ConditionalChange.ONLY_IF_DOES_NOT_EXIST
  }
}

object GeoAddCondition {
  case object OnlyIfExists extends GeoAddCondition
  case object OnlyIfDoesNotExist extends GeoAddCondition
}

case class GeoAddOptions(
    condition: Option[GeoAddCondition] = None,
    changed: Boolean = false
) {
  def toGlide: GlideGeoAddOptions =
    (condition, changed) match {
      case (Some(c), ch) => new GlideGeoAddOptions(c.toGlide, ch)
      case (None, true)  => new GlideGeoAddOptions(true)
      case (None, false) => new GlideGeoAddOptions(false)
    }
}

sealed trait GeoSearchFrom[K] {
  def toGlide(
      encode: K => glide.api.models.GlideString
  ): GlideGeoSearchOrigin.SearchOrigin = this match {
    case GeoSearchFrom.FromMember(member) =>
      new GlideGeoSearchOrigin.MemberOriginBinary(encode(member))
    case GeoSearchFrom.FromCoord(position) =>
      new GlideGeoSearchOrigin.CoordOrigin(position.toGlide)
  }
}

object GeoSearchFrom {
  case class FromMember[K](member: K) extends GeoSearchFrom[K]
  case class FromCoord[K](position: GeoPosition) extends GeoSearchFrom[K]
}

sealed trait GeoSearchBy {
  def toGlide: GlideGeoSearchShape = this match {
    case GeoSearchBy.ByRadius(radius, unit) =>
      new GlideGeoSearchShape(radius, unit.toGlide)
    case GeoSearchBy.ByBox(width, height, unit) =>
      new GlideGeoSearchShape(width, height, unit.toGlide)
  }
}

object GeoSearchBy {
  case class ByRadius(radius: Double, unit: GeoUnit) extends GeoSearchBy
  case class ByBox(width: Double, height: Double, unit: GeoUnit)
      extends GeoSearchBy
}

sealed trait SortOrder {
  def toGlide: GlideSortOrder = this match {
    case SortOrder.Asc  => GlideSortOrder.ASC
    case SortOrder.Desc => GlideSortOrder.DESC
  }
}

object SortOrder {
  case object Asc extends SortOrder
  case object Desc extends SortOrder
}

case class GeoSearchResultOptions(
    sortOrder: Option[SortOrder] = None,
    count: Option[Long] = None,
    any: Boolean = false
) {
  def toGlide: GlideGeoSearchResultOptions =
    (sortOrder, count, any) match {
      case (Some(order), Some(c), a) =>
        new GlideGeoSearchResultOptions(order.toGlide, c, a)
      case (Some(order), None, _) =>
        new GlideGeoSearchResultOptions(order.toGlide)
      case (None, Some(c), a) =>
        new GlideGeoSearchResultOptions(c, a)
      case (None, None, _) =>
        new GlideGeoSearchResultOptions(GlideSortOrder.ASC)
    }
}
