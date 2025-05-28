package io.github.yisrael.valkey4s.internals

import io.github.yisrael.valkey4s.effects.{CopyArgs, Distance, ExpireExistenceArg, GeoCoordinate, GeoHash, GeoRadiusDistStorage, GeoRadiusKeyStorage, GeoRadiusResult, MessageId, RestoreArgs, Score, ScoreWithValue, StreamMessage, XRangePoint, XTrimArgs, ZRange}

import java.util
import scala.concurrent.duration.{Duration, FiniteDuration}

private[valkey4s] trait RedisConversionOps {

  private[valkey4s] implicit class GeoRadiusResultOps[V](v: GeoWithin[V]) {
    def asGeoRadiusResult: GeoRadiusResult[V] =
      GeoRadiusResult[V](
        v.getMember,
        Distance(v.getDistance),
        GeoHash(v.getGeohash),
        GeoCoordinate(v.getCoordinates.getX.doubleValue(), v.getCoordinates.getY.doubleValue())
      )
  }

  private[valkey4s] implicit class GeoRadiusKeyStorageOps[K](v: GeoRadiusKeyStorage[K]) {
    def asGeoRadiusStoreArgs: GeoRadiusStoreArgs[K] = {
      val store: GeoRadiusStoreArgs[_] = GeoRadiusStoreArgs.Builder
        .store[K](v.key)
        .withCount(v.count)
      store.asInstanceOf[GeoRadiusStoreArgs[K]]
    }
  }

  private[valkey4s] implicit class GeoRadiusDistStorageOps[K](v: GeoRadiusDistStorage[K]) {
    def asGeoRadiusStoreArgs: GeoRadiusStoreArgs[K] = {
      val store: GeoRadiusStoreArgs[_] = GeoRadiusStoreArgs.Builder
        .withStoreDist[K](v.key)
        .withCount(v.count)
      store.asInstanceOf[GeoRadiusStoreArgs[K]]
    }
  }

  private[valkey4s] implicit class ZRangeOps[T: Numeric](range: ZRange[T]) {
    def asJavaRange: JRange[Number] = {
      def toJavaNumber(t: T): java.lang.Number = t match {
        case b: Byte  => b
        case s: Short => s
        case i: Int   => i
        case l: Long  => l
        case f: Float => f
        case _        => implicitly[Numeric[T]].toDouble(t)
      }
      val start: Number = toJavaNumber(range.start)
      val end: Number   = toJavaNumber(range.end)
      JRange.create(start, end)
    }
  }

  private[valkey4s] implicit class XTrimArgsOps(args: XTrimArgs) {
    def asJava: JXTrimArgs = {
      val jArgs = args.strategy match {
        case XTrimArgs.Strategy.MAXLEN(threshold) =>
          JXTrimArgs.Builder.maxlen(threshold)
        case XTrimArgs.Strategy.MINID(id) =>
          JXTrimArgs.Builder.minId(id)
      }
      args.precision match {
        case XTrimArgs.Precision.Exact =>
          jArgs.exactTrimming()
        case XTrimArgs.Precision.Approximate(limit) =>
          jArgs.approximateTrimming()
          limit.foreach(jArgs.limit)
      }
      jArgs
    }
  }

  private[valkey4s] implicit class XRangeOps(range: (XRangePoint, XRangePoint)) {
    def asJavaRange: JRange[String] =
      JRange.from(range._1.asJavaBoundary, range._2.asJavaBoundary)
  }

  private[valkey4s] implicit class XRangePointOps(point: XRangePoint) {
    def asJavaBoundary: JRange.Boundary[String] =
      point match {
        case XRangePoint.Unbounded =>
          JRange.Boundary.unbounded()
        case XRangePoint.Inclusive(id) =>
          JRange.Boundary.including(id)
        case XRangePoint.Exclusive(id) =>
          JRange.Boundary.excluding(id)
      }
  }

  private[valkey4s] implicit class StreamMessagesOps[K, V](list: util.List[core.StreamMessage[K, V]]) {
    def toScala: List[StreamMessage[K, V]] =
      list.asScala
        .map(msg => StreamMessage[K, V](MessageId(msg.getId), msg.getStream, msg.getBody.asScala.toMap))
        .toList
  }

  private[valkey4s] implicit class CopyArgOps(underlying: CopyArgs) {
    def asJava: JCopyArgs = {
      val jCopyArgs = new JCopyArgs()
      underlying.destinationDb.foreach(jCopyArgs.destinationDb)
      underlying.replace.foreach(jCopyArgs.replace)
      jCopyArgs
    }
  }

  private[valkey4s] implicit class RestoreArgOps(underlying: RestoreArgs) {

    def asJava: JRestoreArgs = {
      val u = new JRestoreArgs
      underlying.ttl.foreach(u.ttl)
      underlying.replace.foreach(u.replace)
      underlying.absttl.foreach(u.absttl)
      underlying.idleTime.foreach(u.idleTime)
      u
    }
  }

  private[valkey4s] implicit class ExpireExistenceArgOps(underlying: ExpireExistenceArg) {
    def asJava: JExpireArgs = {
      val jExpireArgs = new JExpireArgs()

      underlying match {
        case ExpireExistenceArg.Nx => jExpireArgs.nx()
        case ExpireExistenceArg.Xx => jExpireArgs.xx()
        case ExpireExistenceArg.Gt => jExpireArgs.gt()
        case ExpireExistenceArg.Lt => jExpireArgs.lt()
      }

      jExpireArgs
    }
  }

  private[valkey4s] implicit class ScoredValuesOps[V](v: ScoredValue[V]) {
    def asScoreWithValues: ScoreWithValue[V] = ScoreWithValue[V](Score(v.getScore), v.getValue)
  }

  private[valkey4s] implicit class DurationOps(d: Duration) {
    def toSecondsOrZero: Long = d match {
      case _: Duration.Infinite     => 0
      case duration: FiniteDuration => duration.toSeconds
    }
  }

}