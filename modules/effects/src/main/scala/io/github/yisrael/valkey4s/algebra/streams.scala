

package io.github.yisrael.valkey4s.algebra

import io.github.yisrael.valkey4s.effects.{ MessageId, StreamMessage, XAddArgs, XRangePoint, XReadOffsets, XTrimArgs }

import scala.concurrent.duration.Duration

trait StreamCommands[F[_], K, V] extends StreamGetter[F, K, V] with StreamSetter[F, K, V]

trait StreamGetter[F[_], K, V] {

  def xRead(
      streams: Set[XReadOffsets[K]],
      block: Option[Duration] = None,
      count: Option[Long] = None
  ): F[List[StreamMessage[K, V]]]
  def xRange(key: K, start: XRangePoint, end: XRangePoint, count: Option[Long] = None): F[List[StreamMessage[K, V]]]
  def xRevRange(key: K, start: XRangePoint, end: XRangePoint, count: Option[Long] = None): F[List[StreamMessage[K, V]]]
  def xLen(key: K): F[Long]
}

trait StreamSetter[F[_], K, V] {

  def xAdd(key: K, body: Map[K, V], args: XAddArgs = XAddArgs()): F[MessageId]
  def xTrim(key: K, args: XTrimArgs): F[Long]
  def xDel(key: K, ids: String*): F[Long]
}
