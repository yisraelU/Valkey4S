

package io.github.yisrael.valkey4s.algebra

import io.github.yisrael.valkey4s.effects._
import io.lettuce.core.GeoArgs

// format: off
trait GeoCommands[F[_], K, V] extends GeoGetter[F, K, V] with GeoSetter[F, K, V]

trait GeoGetter[F[_], K, V] {
  def geoDist(key: K, from: V, to: V, unit: GeoArgs.Unit): F[Double]
  def geoHash(key: K, value:V, values: V*): F[List[Option[String]]]
  def geoPos(key: K, value:V,  values: V*): F[List[GeoCoordinate]]
  def geoRadius(key: K, geoRadius: GeoRadius, unit: GeoArgs.Unit): F[Set[V]]
  def geoRadius(key: K, geoRadius: GeoRadius, unit: GeoArgs.Unit, args: GeoArgs): F[List[GeoRadiusResult[V]]]
  def geoRadiusByMember(key: K, value: V, dist: Distance, unit: GeoArgs.Unit): F[Set[V]]
  def geoRadiusByMember(key: K, value: V, dist: Distance, unit: GeoArgs.Unit, args: GeoArgs): F[List[GeoRadiusResult[V]]]
}

trait GeoSetter[F[_], K, V] {
  def geoAdd(key: K, geoValues: GeoLocation[V]*): F[Unit]
  def geoRadius(key: K, geoRadius: GeoRadius, unit: GeoArgs.Unit, storage: GeoRadiusKeyStorage[K]): F[Unit]
  def geoRadius(key: K, geoRadius: GeoRadius, unit: GeoArgs.Unit, storage: GeoRadiusDistStorage[K]): F[Unit]
  def geoRadiusByMember(key: K, value: V, dist: Distance, unit: GeoArgs.Unit, storage: GeoRadiusKeyStorage[K]): F[Unit]
  def geoRadiusByMember(key: K, value: V, dist: Distance, unit: GeoArgs.Unit, storage: GeoRadiusDistStorage[K]): F[Unit]
}
