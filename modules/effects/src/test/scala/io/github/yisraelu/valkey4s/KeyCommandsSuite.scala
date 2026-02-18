package dev.profunktor.valkey4cats

import cats.effect.IO
import dev.profunktor.valkey4cats.model.ValkeyResponse
import dev.profunktor.valkey4cats.model.ValkeyResponse.Ok

class KeyCommandsSuite extends ValkeyTestSuite {

  test("DEL should delete a single key") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("del-test", "value")
        deleted <- valkey.del("del-test")
        result <- valkey.get("del-test")
      } yield {
        assertEquals(deleted, Ok(1L))
        assertEquals(result, Ok(None))
      }
    }
  }

  test("DEL should delete multiple keys") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("del1", "v1")
        _ <- valkey.set("del2", "v2")
        _ <- valkey.set("del3", "v3")
        deleted <- valkey.del("del1", "del2", "del3")
      } yield assertEquals(deleted, Ok(3L))
    }
  }

  test("DEL should return 0 for non-existent keys") {
    valkeyClient.use { valkey =>
      for {
        deleted <- valkey.del("does-not-exist")
      } yield assertEquals(deleted, Ok(0L))
    }
  }

  test("DEL should handle mix of existing and non-existing keys") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("exists", "value")
        deleted <- valkey.del("exists", "does-not-exist")
      } yield assertEquals(deleted, Ok(1L))
    }
  }

  test("EXISTS should return true for existing key") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("exists-test", "value")
        exists <- valkey.exists("exists-test")
        _ <- valkey.del("exists-test")
      } yield assertEquals(exists, Ok(true))
    }
  }

  test("EXISTS should return false for non-existent key") {
    valkeyClient.use { valkey =>
      for {
        exists <- valkey.exists("does-not-exist")
      } yield assertEquals(exists, Ok(false))
    }
  }

  test("EXISTSMANY should count existing keys") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("key1", "v1")
        _ <- valkey.set("key2", "v2")
        count <- valkey.existsMany("key1", "key2", "key3")
        _ <- valkey.del("key1", "key2")
      } yield assertEquals(count, Ok(2L))
    }
  }

  test("EXISTSMANY should return 0 when no keys exist") {
    valkeyClient.use { valkey =>
      for {
        count <- valkey.existsMany("none1", "none2", "none3")
      } yield assertEquals(count, Ok(0L))
    }
  }

  test("complex workflow: create, check, delete") {
    valkeyClient.use { valkey =>
      for {
        // Create some keys
        _ <- valkey.mSet(Map("wf1" -> "v1", "wf2" -> "v2", "wf3" -> "v3"))

        // Check they exist
        count1 <- valkey.existsMany("wf1", "wf2", "wf3")

        // Delete one
        deleted <- valkey.del("wf2")

        // Check count again
        count2 <- valkey.existsMany("wf1", "wf2", "wf3")

        // Verify individual existence
        exists1 <- valkey.exists("wf1")
        exists2 <- valkey.exists("wf2")
        exists3 <- valkey.exists("wf3")

        // Cleanup
        _ <- valkey.del("wf1", "wf3")
      } yield {
        assertEquals(count1, Ok(3L))
        assertEquals(deleted, Ok(1L))
        assertEquals(count2, Ok(2L))
        assertEquals(exists1, Ok(true))
        assertEquals(exists2, Ok(false))
        assertEquals(exists3, Ok(true))
      }
    }
  }

  test("stress test: delete many keys at once") {
    valkeyClient.use { valkey =>
      for {
        // Create 100 keys
        _ <- IO.traverse((1 to 100).toList)(i =>
          valkey.set(s"stress-$i", s"value-$i")
        )

        // Verify they exist
        count1 <- valkey.existsMany((1 to 100).map(i => s"stress-$i"): _*)

        // Delete all at once
        deleted <- valkey.del((1 to 100).map(i => s"stress-$i"): _*)

        // Verify deletion
        count2 <- valkey.existsMany((1 to 100).map(i => s"stress-$i"): _*)
      } yield {
        assertEquals(count1, Ok(100L))
        assertEquals(deleted, Ok(100L))
        assertEquals(count2, Ok(0L))
      }
    }
  }
}
