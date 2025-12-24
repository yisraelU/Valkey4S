package dev.profunktor.valkey4cats.examples

import cats.effect.*
import dev.profunktor.valkey4cats.Valkey
import dev.profunktor.valkey4cats.effect.Log

/** Basic usage example for Valkey4S
  *
  * To run this example, ensure you have Valkey or Redis running on localhost:6379
  */
object BasicExample extends IOApp.Simple {

  // Use stdout logging
  implicit val logger: Log[IO] = Log.Stdout.instance[IO]

  def run: IO[Unit] = {
    // Create a connection and use it
    Valkey[IO].utf8("redis://localhost:6379").use { valkey =>
      for {
        // Basic SET/GET
        _ <- IO.println("=== Basic String Operations ===")
        _ <- valkey.set("mykey", "Hello, Valkey4S!")
        value <- valkey.get("mykey")
        _ <- IO.println(s"GET mykey: $value")

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
        _ <- IO.println(s"Users: $users")

        // INCR/DECR
        _ <- IO.println("\n=== Counter Operations ===")
        _ <- valkey.set("counter", "0")
        c1 <- valkey.incr("counter")
        _ <- IO.println(s"After INCR: $c1")
        c2 <- valkey.incrBy("counter", 10)
        _ <- IO.println(s"After INCRBY 10: $c2")
        c3 <- valkey.decr("counter")
        _ <- IO.println(s"After DECR: $c3")

        // Key operations
        _ <- IO.println("\n=== Key Operations ===")
        exists <- valkey.exists("mykey")
        _ <- IO.println(s"Key 'mykey' exists: $exists")
        deleted <- valkey.del("mykey", "counter")
        _ <- IO.println(s"Deleted $deleted keys")
        existsAfter <- valkey.exists("mykey")
        _ <- IO.println(s"Key 'mykey' exists after delete: $existsAfter")

        _ <- IO.println("\n=== Example completed successfully! ===")
      } yield ()
    }
  }
}
