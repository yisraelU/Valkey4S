package dev.profunktor.valkey4cats

import dev.profunktor.valkey4cats.model.ValkeyResponse
import dev.profunktor.valkey4cats.model.ValkeyResponse.Ok

class HashCommandsSuite extends ValkeyTestSuite {

  test("HSET should set a single field-value pair") {
    valkeyClient.use { valkey =>
      for {
        count <- valkey.hset("hash1", Map("field1" -> "value1"))
        value <- valkey.hget("hash1", "field1")
        _ <- valkey.del("hash1")
      } yield {
        assertEquals(count, Ok(1L))
        assertEquals(value, Ok(Some("value1")))
      }
    }
  }

  test("HSET should set multiple field-value pairs") {
    valkeyClient.use { valkey =>
      for {
        count <- valkey.hset(
          "hash2",
          Map("f1" -> "v1", "f2" -> "v2", "f3" -> "v3")
        )
        _ <- valkey.del("hash2")
      } yield assertEquals(count, Ok(3L))
    }
  }

  test("HSET should update existing field and return 0") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash3", Map("field" -> "original"))
        count <- valkey.hset("hash3", Map("field" -> "updated"))
        value <- valkey.hget("hash3", "field")
        _ <- valkey.del("hash3")
      } yield {
        assertEquals(count, Ok(0L))
        assertEquals(value, Ok(Some("updated")))
      }
    }
  }

  test("HGET should return None for non-existent field") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash4", Map("exists" -> "value"))
        result <- valkey.hget("hash4", "does-not-exist")
        _ <- valkey.del("hash4")
      } yield assertEquals(result, Ok(None))
    }
  }

  test("HGET should return None for non-existent hash") {
    valkeyClient.use { valkey =>
      for {
        result <- valkey.hget("non-existent-hash", "field")
      } yield assertEquals(result, Ok(None))
    }
  }

  test("HGETALL should return all field-value pairs") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash5", Map("f1" -> "v1", "f2" -> "v2", "f3" -> "v3"))
        result <- valkey.hgetall("hash5")
        _ <- valkey.del("hash5")
      } yield {
        val Ok(m) = result: @unchecked
        assertEquals(m.size, 3)
        assertEquals(m("f1"), "v1")
        assertEquals(m("f2"), "v2")
        assertEquals(m("f3"), "v3")
      }
    }
  }

  test("HGETALL should return empty map for non-existent hash") {
    valkeyClient.use { valkey =>
      for {
        result <- valkey.hgetall("non-existent")
      } yield assertEquals(result, Ok(Map.empty[String, String]))
    }
  }

  test("HMGET should return values for multiple fields") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash6", Map("f1" -> "v1", "f2" -> "v2", "f3" -> "v3"))
        result <- valkey.hmget("hash6", "f1", "f2", "f3")
        _ <- valkey.del("hash6")
      } yield {
        assertEquals(result, Ok(List(Some("v1"), Some("v2"), Some("v3"))))
      }
    }
  }

  test("HMGET should return None for non-existent fields") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash7", Map("exists" -> "value"))
        result <- valkey.hmget("hash7", "exists", "missing1", "missing2")
        _ <- valkey.del("hash7")
      } yield {
        assertEquals(result, Ok(List(Some("value"), None, None)))
      }
    }
  }

  test("HDEL should delete specified fields") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash8", Map("f1" -> "v1", "f2" -> "v2", "f3" -> "v3"))
        count <- valkey.hdel("hash8", "f1", "f2")
        remaining <- valkey.hgetall("hash8")
        _ <- valkey.del("hash8")
      } yield {
        assertEquals(count, Ok(2L))
        assertEquals(remaining, Ok(Map("f3" -> "v3")))
      }
    }
  }

  test("HDEL should return 0 for non-existent fields") {
    valkeyClient.use { valkey =>
      for {
        count <- valkey.hdel("non-existent", "field")
      } yield assertEquals(count, Ok(0L))
    }
  }

  test("HEXISTS should return true for existing field") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash9", Map("field" -> "value"))
        exists <- valkey.hexists("hash9", "field")
        _ <- valkey.del("hash9")
      } yield assertEquals(exists, Ok(true))
    }
  }

  test("HEXISTS should return false for non-existent field") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash10", Map("field" -> "value"))
        exists <- valkey.hexists("hash10", "other-field")
        _ <- valkey.del("hash10")
      } yield assertEquals(exists, Ok(false))
    }
  }

  test("HKEYS should return all field names") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset(
          "hash11",
          Map("f1" -> "v1", "f2" -> "v2", "f3" -> "v3")
        )
        keys <- valkey.hkeys("hash11")
        _ <- valkey.del("hash11")
      } yield {
        val Ok(ks) = keys: @unchecked
        assertEquals(ks.toSet, Set("f1", "f2", "f3"))
      }
    }
  }

  test("HVALS should return all values") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset(
          "hash12",
          Map("f1" -> "v1", "f2" -> "v2", "f3" -> "v3")
        )
        values <- valkey.hvals("hash12")
        _ <- valkey.del("hash12")
      } yield {
        val Ok(vs) = values: @unchecked
        assertEquals(vs.toSet, Set("v1", "v2", "v3"))
      }
    }
  }

  test("HLEN should return number of fields") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset(
          "hash13",
          Map("f1" -> "v1", "f2" -> "v2", "f3" -> "v3")
        )
        length <- valkey.hlen("hash13")
        _ <- valkey.del("hash13")
      } yield assertEquals(length, Ok(3L))
    }
  }

  test("HLEN should return 0 for non-existent hash") {
    valkeyClient.use { valkey =>
      for {
        length <- valkey.hlen("non-existent")
      } yield assertEquals(length, Ok(0L))
    }
  }

  test("HINCRBY should increment integer field") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash14", Map("counter" -> "10"))
        result <- valkey.hincrBy("hash14", "counter", 5)
        value <- valkey.hget("hash14", "counter")
        _ <- valkey.del("hash14")
      } yield {
        assertEquals(result, Ok(15L))
        assertEquals(value, Ok(Some("15")))
      }
    }
  }

  test("HINCRBY should initialize non-existent field to increment value") {
    valkeyClient.use { valkey =>
      for {
        result <- valkey.hincrBy("hash15", "new-counter", 42)
        _ <- valkey.del("hash15")
      } yield assertEquals(result, Ok(42L))
    }
  }

  test("HINCRBYFLOAT should increment float field") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash16", Map("price" -> "10.5"))
        result <- valkey.hincrByFloat("hash16", "price", 2.3)
        _ <- valkey.del("hash16")
      } yield {
        val Ok(d) = result: @unchecked
        assertEquals(d, 12.8, 0.001)
      }
    }
  }

  test("HSETNX should set field only if it doesn't exist") {
    valkeyClient.use { valkey =>
      for {
        result1 <- valkey.hsetnx("hash17", "field", "value1")
        result2 <- valkey.hsetnx("hash17", "field", "value2")
        value <- valkey.hget("hash17", "field")
        _ <- valkey.del("hash17")
      } yield {
        assertEquals(result1, Ok(true))
        assertEquals(result2, Ok(false))
        assertEquals(value, Ok(Some("value1")))
      }
    }
  }

  test("HSTRLEN should return length of field value") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash18", Map("field" -> "Hello"))
        length <- valkey.hstrlen("hash18", "field")
        _ <- valkey.del("hash18")
      } yield assertEquals(length, Ok(5L))
    }
  }

  test("HSTRLEN should return 0 for non-existent field") {
    valkeyClient.use { valkey =>
      for {
        length <- valkey.hstrlen("hash19", "non-existent")
      } yield assertEquals(length, Ok(0L))
    }
  }

  test("HRANDFIELD should return random field from hash") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset(
          "hash20",
          Map("f1" -> "v1", "f2" -> "v2", "f3" -> "v3")
        )
        field <- valkey.hrandfield("hash20")
        _ <- valkey.del("hash20")
      } yield {
        val Ok(f) = field: @unchecked
        assert(f.isDefined)
        assert(Set("f1", "f2", "f3").contains(f.get))
      }
    }
  }

  test("HRANDFIELD should return None for non-existent hash") {
    valkeyClient.use { valkey =>
      for {
        field <- valkey.hrandfield("non-existent")
      } yield assertEquals(field, Ok(None))
    }
  }

  test("HRANDFIELDWITHCOUNT should return multiple random fields") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset(
          "hash21",
          Map("f1" -> "v1", "f2" -> "v2", "f3" -> "v3")
        )
        fields <- valkey.hrandfieldWithCount("hash21", 2)
        _ <- valkey.del("hash21")
      } yield {
        val Ok(fs) = fields: @unchecked
        assertEquals(fs.length, 2)
        assert(fs.forall(f => Set("f1", "f2", "f3").contains(f)))
      }
    }
  }

  test("HRANDFIELDWITHCOUNTWITHVALUES should return random field-value pairs") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset(
          "hash22",
          Map("f1" -> "v1", "f2" -> "v2", "f3" -> "v3")
        )
        pairs <- valkey.hrandfieldWithCountWithValues("hash22", 2)
        _ <- valkey.del("hash22")
      } yield {
        val Ok(ps) = pairs: @unchecked
        assertEquals(ps.length, 2)
        ps.foreach { case (field, value) =>
          assert(Set("f1", "f2", "f3").contains(field))
          assertEquals(value, s"v${field.last}")
        }
      }
    }
  }

  test("complex workflow: hash as user profile") {
    valkeyClient.use { valkey =>
      for {
        // Create user profile
        _ <- valkey.hset(
          "user:1",
          Map(
            "name" -> "Alice",
            "email" -> "alice@example.com",
            "age" -> "30",
            "score" -> "100"
          )
        )

        // Get specific fields
        name <- valkey.hget("user:1", "name")
        email <- valkey.hget("user:1", "email")

        // Increment score
        newScore <- valkey.hincrBy("user:1", "score", 50)

        // Check if field exists
        hasPhone <- valkey.hexists("user:1", "phone")

        // Get all fields
        allFields <- valkey.hgetall("user:1")

        // Count fields
        fieldCount <- valkey.hlen("user:1")

        // Cleanup
        _ <- valkey.del("user:1")
      } yield {
        assertEquals(name, Ok(Some("Alice")))
        assertEquals(email, Ok(Some("alice@example.com")))
        assertEquals(newScore, Ok(150L))
        assertEquals(hasPhone, Ok(false))
        val Ok(af) = allFields: @unchecked
        assertEquals(af.size, 4)
        assertEquals(fieldCount, Ok(4L))
      }
    }
  }
}
