package dev.profunktor.valkey4cats

import dev.profunktor.valkey4cats.model.ValkeyResponse
import dev.profunktor.valkey4cats.model.ValkeyResponse.Ok

class StringCommandsSuite extends ValkeyTestSuite {

  test("GET should return None for non-existent key") {
    valkeyClient.use { valkey =>
      for {
        result <- valkey.get("non-existent-key")
      } yield assertEquals(result, Ok(None))
    }
  }

  test("SET and GET should work for simple string") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("test-key", "test-value")
        result <- valkey.get("test-key")
        _ <- valkey.del("test-key") // Cleanup
      } yield assertEquals(result, Ok(Some("test-value")))
    }
  }

  test("SET and GET should handle UTF-8 characters") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("utf8-key", "Hello 世界 🌍")
        result <- valkey.get("utf8-key")
        _ <- valkey.del("utf8-key")
      } yield assertEquals(result, Ok(Some("Hello 世界 🌍")))
    }
  }

  test("SET should overwrite existing value") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("overwrite-key", "original")
        _ <- valkey.set("overwrite-key", "updated")
        result <- valkey.get("overwrite-key")
        _ <- valkey.del("overwrite-key")
      } yield assertEquals(result, Ok(Some("updated")))
    }
  }

  test("MGET should return values for existing keys") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("key1", "value1")
        _ <- valkey.set("key2", "value2")
        _ <- valkey.set("key3", "value3")
        result <- valkey.mGet(Set("key1", "key2", "key3"))
        _ <- valkey.del("key1", "key2", "key3")
      } yield {
        val Ok(m) = result: @unchecked
        assertEquals(m.size, 3)
        assertEquals(m("key1"), "value1")
        assertEquals(m("key2"), "value2")
        assertEquals(m("key3"), "value3")
      }
    }
  }

  test("MGET should handle mix of existing and non-existing keys") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("exists", "value")
        result <- valkey.mGet(Set("exists", "does-not-exist"))
        _ <- valkey.del("exists")
      } yield {
        val Ok(m) = result: @unchecked
        assertEquals(m.size, 1)
        assertEquals(m.get("exists"), Some("value"))
        assertEquals(m.get("does-not-exist"), None)
      }
    }
  }

  test("MSET should set multiple key-value pairs") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.mSet(Map("mset1" -> "v1", "mset2" -> "v2", "mset3" -> "v3"))
        v1 <- valkey.get("mset1")
        v2 <- valkey.get("mset2")
        v3 <- valkey.get("mset3")
        _ <- valkey.del("mset1", "mset2", "mset3")
      } yield {
        assertEquals(v1, Ok(Some("v1")))
        assertEquals(v2, Ok(Some("v2")))
        assertEquals(v3, Ok(Some("v3")))
      }
    }
  }

  test("INCR should increment a numeric string") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("counter", "10")
        result <- valkey.incr("counter")
        _ <- valkey.del("counter")
      } yield assertEquals(result, Ok(11L))
    }
  }

  test("INCR should initialize non-existent key to 1") {
    valkeyClient.use { valkey =>
      for {
        result <- valkey.incr("new-counter")
        _ <- valkey.del("new-counter")
      } yield assertEquals(result, Ok(1L))
    }
  }

  test("INCRBY should increment by specified amount") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("incrby-test", "100")
        result <- valkey.incrBy("incrby-test", 50)
        _ <- valkey.del("incrby-test")
      } yield assertEquals(result, Ok(150L))
    }
  }

  test("DECR should decrement a numeric string") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("decr-test", "10")
        result <- valkey.decr("decr-test")
        _ <- valkey.del("decr-test")
      } yield assertEquals(result, Ok(9L))
    }
  }

  test("DECRBY should decrement by specified amount") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("decrby-test", "100")
        result <- valkey.decrBy("decrby-test", 30)
        _ <- valkey.del("decrby-test")
      } yield assertEquals(result, Ok(70L))
    }
  }

  test("APPEND should append to existing value") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("append-test", "Hello")
        length <- valkey.append("append-test", " World")
        result <- valkey.get("append-test")
        _ <- valkey.del("append-test")
      } yield {
        assertEquals(length, Ok(11L)) // "Hello World".length
        assertEquals(result, Ok(Some("Hello World")))
      }
    }
  }

  test("APPEND should create key if it doesn't exist") {
    valkeyClient.use { valkey =>
      for {
        length <- valkey.append("new-append", "Created")
        result <- valkey.get("new-append")
        _ <- valkey.del("new-append")
      } yield {
        assertEquals(length, Ok(7L))
        assertEquals(result, Ok(Some("Created")))
      }
    }
  }

  test("STRLEN should return length of string value") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.set("strlen-test", "Hello")
        length <- valkey.strlen("strlen-test")
        _ <- valkey.del("strlen-test")
      } yield assertEquals(length, Ok(5L))
    }
  }

  test("STRLEN should return 0 for non-existent key") {
    valkeyClient.use { valkey =>
      for {
        length <- valkey.strlen("does-not-exist")
      } yield assertEquals(length, Ok(0L))
    }
  }

  test("complex workflow: counters with conditions") {
    valkeyClient.use { valkey =>
      for {
        // Initialize counter
        _ <- valkey.set("workflow-counter", "0")

        // Increment multiple times
        c1 <- valkey.incr("workflow-counter")
        c2 <- valkey.incrBy("workflow-counter", 10)
        c3 <- valkey.incr("workflow-counter")

        // Check final value
        finalValue <- valkey.get("workflow-counter")

        // Cleanup
        _ <- valkey.del("workflow-counter")
      } yield {
        assertEquals(c1, Ok(1L))
        assertEquals(c2, Ok(11L))
        assertEquals(c3, Ok(12L))
        assertEquals(finalValue, Ok(Some("12")))
      }
    }
  }
}
