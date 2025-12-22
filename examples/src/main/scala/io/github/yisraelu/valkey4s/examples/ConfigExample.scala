package io.github.yisraelu.valkey4s.examples

import cats.effect._
import io.github.yisraelu.valkey4s.Valkey
import io.github.yisraelu.valkey4s.codec.ValkeyCodec.utf8Codec
import io.github.yisraelu.valkey4s.effect.Log
import io.github.yisraelu.valkey4s.model._
import scala.concurrent.duration._

/** Advanced configuration example showing various client options */
object ConfigExample extends IOApp.Simple {

  implicit val logger: Log[IO] = Log.Stdout.instance[IO]

  def run: IO[Unit] = {
    // Example 1: Simple URI parsing
    val simpleExample = Valkey[IO].utf8("redis://localhost:6379")

    // Example 2: URI with authentication
    val authExample = Valkey[IO].utf8("redis://:mypassword@localhost:6379")

    // Example 3: TLS connection
    val tlsExample = Valkey[IO].utf8("rediss://secure-server:6380")

    // Example 4: Full configuration with all options
    val config = ValkeyClientConfig(
      addresses = List(NodeAddress("localhost", 6379)),
      useTls = false,
      requestTimeout = Some(5.seconds),
      credentials = Some(ServerCredentials.password("mypassword")),
      readFrom = Some(ReadFromStrategy.Primary),
      reconnectStrategy = Some(
        BackOffStrategy.ExponentialBackoff(
          numOfRetries = 5,
          baseFactor = 100.millis,
          exponentBase = 2,
          jitterPercent = 10
        )
      ),
      databaseId = Some(0),
      clientName = Some("my-app"),
      protocolVersion = ProtocolVersion.RESP3
    )

    val advancedExample = Valkey[IO].fromConfig[String, String](config)

    // Use the simple example
    simpleExample.use { valkey =>
      for {
        _ <- IO.println("=== Configuration Examples ===")
        _ <- valkey.set("config-test", "success")
        result <- valkey.get("config-test")
        _ <- IO.println(s"Result: $result")
        _ <- valkey.del("config-test")
      } yield ()
    }
  }
}
