package io.github.yisraelu.valkey4s.model

import cats.implicits.toBifunctorOps

import java.net.URI
import scala.util.Try

/** Represents a parsed Valkey/Redis URI
  *
  * Supports URI schemes:
  * - valkey:// (native Valkey, standard connection)
  * - valkeys:// (native Valkey, TLS connection)
  * - redis:// (legacy compatibility, standard connection)
  * - rediss:// (legacy compatibility, TLS connection)
  */
sealed abstract class ValkeyUri {

  /** The URI scheme (valkey, valkeys, redis, rediss) */
  def scheme: ValkeyUri.Scheme

  /** The server host */
  def host: String

  /** The server port */
  def port: Int

  /** Optional authentication credentials */
  def credentials: Option[ServerCredentials]

  /** Optional database number (0-15) */
  def database: Option[Int]

  /** Whether this URI requires TLS */
  def useTls: Boolean = scheme.requiresTls

  /** Convert to a standard URI string */
  def toUriString: String = {
    val auth = credentials match {
      case Some(p: ServerCredentials.Password) => s":${p.password}@"
      case Some(up: ServerCredentials.UsernamePassword) =>
        s"${up.username}:${up.password}@"
      case None => ""
    }
    val db = database.map(d => s"/$d").getOrElse("")
    s"${scheme.name}://$auth$host:$port$db"
  }
}

object ValkeyUri {

  private final case class ValkeyUriImpl(
      scheme: Scheme,
      host: String,
      port: Int,
      credentials: Option[ServerCredentials] = None,
      database: Option[Int] = None
  ) extends ValkeyUri

  /** Create a ValkeyUri */
  def apply(
      scheme: Scheme,
      host: String,
      port: Int,
      credentials: Option[ServerCredentials] = None,
      database: Option[Int] = None
  ): ValkeyUri =
    ValkeyUriImpl(scheme, host, port, credentials, database)

  /** Pattern matching support */
  def unapply(
      uri: ValkeyUri
  ): Option[(Scheme, String, Int, Option[ServerCredentials], Option[Int])] =
    Some((uri.scheme, uri.host, uri.port, uri.credentials, uri.database))

  /** URI scheme variants */
  sealed trait Scheme { self =>

    def name: String = self match {
      case Scheme.Valkey  => "valkey"
      case Scheme.Valkeys => "valkeys"
      case Scheme.Redis   => "redis"
      case Scheme.Rediss  => "rediss"
    }

    def requiresTls: Boolean = self match {
      case Scheme.Valkey  => false
      case Scheme.Valkeys => true
      case Scheme.Redis   => false
      case Scheme.Rediss  => true
    }
  }

  object Scheme {
    case object Valkey extends Scheme
    case object Valkeys extends Scheme
    case object Redis extends Scheme
    case object Rediss extends Scheme

    def fromString(s: String): Either[String, Scheme] = s match {
      case "valkey"  => Right(Valkey)
      case "valkeys" => Right(Valkeys)
      case "redis"   => Right(Redis)
      case "rediss"  => Right(Rediss)
      case other     => Left(s"Invalid scheme $other")
    }
  }

  /** Parse a URI string into a ValkeyUri
    *
    * @param uriString The URI string to parse
    * @return Either an error message or the parsed URI
    */
  def fromString(uriString: String): Either[String, ValkeyUri] = {

    for {
      parsed <- Try { new URI(uriString) }.toEither.leftMap(ex =>
        s"invalid uri ex: $ex"
      )
      scheme <- Scheme.fromString(parsed.getScheme)
    } yield {
      val host = Option(parsed.getHost).getOrElse("localhost")
      val port =
        if (parsed.getPort > 0) parsed.getPort else NodeAddress.DefaultPort

      // Parse credentials from userInfo
      val credentials: Option[ServerCredentials] =
        Option(parsed.getUserInfo).flatMap { userInfo =>
          userInfo.split(":", 2) match {
            case Array("", password) =>
              Some(ServerCredentials.password(password))
            case Array(username, password) =>
              Some(ServerCredentials.usernamePassword(username, password))
            case _ => None
          }
        }

      // Parse database from path
      val database: Option[Int] = Option(parsed.getPath)
        .filter(_.nonEmpty)
        .map(_.stripPrefix("/"))
        .filter(_.nonEmpty)
        .flatMap(db => scala.util.Try(db.toInt).toOption)

      ValkeyUri(
        scheme = scheme,
        host = host,
        port = port,
        credentials = credentials,
        database = database
      )
    }
  }
}
