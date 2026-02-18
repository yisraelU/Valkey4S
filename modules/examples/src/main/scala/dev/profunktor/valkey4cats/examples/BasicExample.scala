package dev.profunktor.valkey4cats.examples

import cats.effect.*
import dev.profunktor.valkey4cats.Valkey
import dev.profunktor.valkey4cats.effect.Log
import dev.profunktor.valkey4cats.model.ValkeyResponse
import dev.profunktor.valkey4cats.model.ValkeyResponse.{Ok, Err}

/** Basic usage example for Valkey4S
  *
  * To run this example, ensure you have Valkey or Redis running on localhost:6379
  *
  * Every command returns F[ValkeyResponse[A]] where:
  *   - ValkeyResponse.Ok(value) — command succeeded
  *   - ValkeyResponse.Err(error) — domain-level error (WRONGTYPE, READONLY, etc.)
  *   - Infrastructure errors (timeout, connection lost) propagate in F
  */
object BasicExample extends IOApp.Simple {

  // Use stdout logging
  implicit val logger: Log[IO] = Log.Stdout.instance[IO]

  def run: IO[Unit] = {
    // Create a connection and use it
    Valkey[IO].utf8("redis://localhost:6379").use { valkey =>
      for {
        // Basic SET/GET — pattern match on ValkeyResponse
        _ <- IO.println("=== Basic String Operations ===")
        _ <- valkey.set("mykey", "Hello, Valkey4S!")
        value <- valkey.get("mykey")
        _ <- value match {
          case Ok(Some(v)) => IO.println(s"GET mykey: $v")
          case Ok(None)    => IO.println("GET mykey: (not found)")
          case Err(e)      => IO.println(s"GET mykey failed: ${e.message}")
        }

        // MSET/MGET
        _ <- IO.println("\n=== Multi-key Operations ===")
        _ <- valkey.mSet(
          Map(
            "user:1:name" -> "Alice",
            "user:2:name" -> "Bob",
            "user:3:name" -> "Charlie"
          )
        )
        users <- valkey.mGet(Set("user:1:name", "user:2:name", "user:3:name"))
        _ <- IO.println(s"Users: ${users.toOption.getOrElse(Map.empty)}")

        // INCR/DECR — use fold for concise handling
        _ <- IO.println("\n=== Counter Operations ===")
        _ <- valkey.set("counter", "0")
        c1 <- valkey.incr("counter")
        _ <- IO.println(
          s"After INCR: ${c1.fold(e => s"error: ${e.message}", _.toString)}"
        )
        c2 <- valkey.incrBy("counter", 10)
        _ <- IO.println(
          s"After INCRBY 10: ${c2.fold(e => s"error: ${e.message}", _.toString)}"
        )
        c3 <- valkey.decr("counter")
        _ <- IO.println(
          s"After DECR: ${c3.fold(e => s"error: ${e.message}", _.toString)}"
        )

        // Key operations
        _ <- IO.println("\n=== Key Operations ===")
        exists <- valkey.exists("mykey")
        _ <- IO.println(s"Key 'mykey' exists: ${exists.getOrElse(false)}")
        deleted <- valkey.del("mykey", "counter")
        _ <- IO.println(s"Deleted ${deleted.getOrElse(0L)} keys")
        existsAfter <- valkey.exists("mykey")
        _ <- IO.println(
          s"Key 'mykey' exists after delete: ${existsAfter.getOrElse(false)}"
        )

        _ <- IO.println("\n=== Example completed successfully! ===")
      } yield ()
    }
  }
}
