package io.github.yisraelu.valkey4s.codec

import glide.api.models.GlideString

import java.nio.charset.StandardCharsets

/** Type class for encoding/decoding values to/from Glide's GlideString format
  *
  * @tparam A The type to encode/decode
  */
trait ValkeyCodec[A] {
  def encode(value: A): GlideString
  def decode(gs: GlideString): A
}

object ValkeyCodec {

  def apply[A](implicit codec: ValkeyCodec[A]): ValkeyCodec[A] = codec

  /** String codec using UTF-8 encoding */
  implicit val utf8Codec: ValkeyCodec[String] = new ValkeyCodec[String] {
    def encode(value: String): GlideString =
      GlideString.of(value.getBytes(StandardCharsets.UTF_8))

    def decode(gs: GlideString): String =
      new String(gs.getBytes(), StandardCharsets.UTF_8)
  }

  /** Byte array codec (pass-through) */
  implicit val byteArrayCodec: ValkeyCodec[Array[Byte]] =
    new ValkeyCodec[Array[Byte]] {
      def encode(value: Array[Byte]): GlideString =
        GlideString.of(value)

      def decode(gs: GlideString): Array[Byte] =
        gs.getBytes()
    }

  /** Long codec (stores as string representation) */
  implicit val longCodec: ValkeyCodec[Long] = new ValkeyCodec[Long] {
    def encode(value: Long): GlideString =
      utf8Codec.encode(value.toString)

    def decode(gs: GlideString): Long =
      utf8Codec.decode(gs).toLong
  }

  /** Int codec (stores as string representation) */
  implicit val intCodec: ValkeyCodec[Int] = new ValkeyCodec[Int] {
    def encode(value: Int): GlideString =
      utf8Codec.encode(value.toString)

    def decode(gs: GlideString): Int =
      utf8Codec.decode(gs).toInt
  }

  /** Double codec (stores as string representation) */
  implicit val doubleCodec: ValkeyCodec[Double] = new ValkeyCodec[Double] {
    def encode(value: Double): GlideString =
      utf8Codec.encode(value.toString)

    def decode(gs: GlideString): Double =
      utf8Codec.decode(gs).toDouble
  }
}
