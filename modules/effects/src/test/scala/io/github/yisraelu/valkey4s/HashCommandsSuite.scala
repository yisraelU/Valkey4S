package dev.profunktor.valkey4cats

class HashCommandsSuite extends ValkeyTestSuite {

  test("HSET should set a single field-value pair") {
    valkeyClient.use { valkey =>
      for {
        count <- valkey.hset("hash1", Map("field1" -> "value1"))
        value <- valkey.hget("hash1", "field1")
        _ <- valkey.del("hash1")
      } yield {
        assertEquals(count, 1L)
        assertEquals(value, Some("value1"))
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
      } yield assertEquals(count, 3L)
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
        assertEquals(count, 0L)
        assertEquals(value, Some("updated"))
      }
    }
  }

  test("HGET should return None for non-existent field") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash4", Map("exists" -> "value"))
        result <- valkey.hget("hash4", "does-not-exist")
        _ <- valkey.del("hash4")
      } yield assertEquals(result, None)
    }
  }

  test("HGET should return None for non-existent hash") {
    valkeyClient.use { valkey =>
      for {
        result <- valkey.hget("non-existent-hash", "field")
      } yield assertEquals(result, None)
    }
  }

  test("HGETALL should return all field-value pairs") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash5", Map("f1" -> "v1", "f2" -> "v2", "f3" -> "v3"))
        result <- valkey.hgetall("hash5")
        _ <- valkey.del("hash5")
      } yield {
        assertEquals(result.size, 3)
        assertEquals(result("f1"), "v1")
        assertEquals(result("f2"), "v2")
        assertEquals(result("f3"), "v3")
      }
    }
  }

  test("HGETALL should return empty map for non-existent hash") {
    valkeyClient.use { valkey =>
      for {
        result <- valkey.hgetall("non-existent")
      } yield assertEquals(result, Map.empty[String, String])
    }
  }

  test("HMGET should return values for multiple fields") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash6", Map("f1" -> "v1", "f2" -> "v2", "f3" -> "v3"))
        result <- valkey.hmget("hash6", "f1", "f2", "f3")
        _ <- valkey.del("hash6")
      } yield {
        assertEquals(result, List(Some("v1"), Some("v2"), Some("v3")))
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
        assertEquals(result, List(Some("value"), None, None))
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
        assertEquals(count, 2L)
        assertEquals(remaining, Map("f3" -> "v3"))
      }
    }
  }

  test("HDEL should return 0 for non-existent fields") {
    valkeyClient.use { valkey =>
      for {
        count <- valkey.hdel("non-existent", "field")
      } yield assertEquals(count, 0L)
    }
  }

  test("HEXISTS should return true for existing field") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash9", Map("field" -> "value"))
        exists <- valkey.hexists("hash9", "field")
        _ <- valkey.del("hash9")
      } yield assertEquals(exists, true)
    }
  }

  test("HEXISTS should return false for non-existent field") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash10", Map("field" -> "value"))
        exists <- valkey.hexists("hash10", "other-field")
        _ <- valkey.del("hash10")
      } yield assertEquals(exists, false)
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
        assertEquals(keys.toSet, Set("f1", "f2", "f3"))
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
        assertEquals(values.toSet, Set("v1", "v2", "v3"))
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
      } yield assertEquals(length, 3L)
    }
  }

  test("HLEN should return 0 for non-existent hash") {
    valkeyClient.use { valkey =>
      for {
        length <- valkey.hlen("non-existent")
      } yield assertEquals(length, 0L)
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
        assertEquals(result, 15L)
        assertEquals(value, Some("15"))
      }
    }
  }

  test("HINCRBY should initialize non-existent field to increment value") {
    valkeyClient.use { valkey =>
      for {
        result <- valkey.hincrBy("hash15", "new-counter", 42)
        _ <- valkey.del("hash15")
      } yield assertEquals(result, 42L)
    }
  }

  test("HINCRBYFLOAT should increment float field") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash16", Map("price" -> "10.5"))
        result <- valkey.hincrByFloat("hash16", "price", 2.3)
        _ <- valkey.del("hash16")
      } yield assertEquals(result, 12.8, 0.001)
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
        assertEquals(result1, true)
        assertEquals(result2, false)
        assertEquals(value, Some("value1"))
      }
    }
  }

  test("HSTRLEN should return length of field value") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.hset("hash18", Map("field" -> "Hello"))
        length <- valkey.hstrlen("hash18", "field")
        _ <- valkey.del("hash18")
      } yield assertEquals(length, 5L)
    }
  }

  test("HSTRLEN should return 0 for non-existent field") {
    valkeyClient.use { valkey =>
      for {
        length <- valkey.hstrlen("hash19", "non-existent")
      } yield assertEquals(length, 0L)
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
        assert(field.isDefined)
        assert(Set("f1", "f2", "f3").contains(field.get))
      }
    }
  }

  test("HRANDFIELD should return None for non-existent hash") {
    valkeyClient.use { valkey =>
      for {
        field <- valkey.hrandfield("non-existent")
      } yield assertEquals(field, None)
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
        assertEquals(fields.length, 2)
        assert(fields.forall(f => Set("f1", "f2", "f3").contains(f)))
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
        assertEquals(pairs.length, 2)
        pairs.foreach { case (field, value) =>
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
        assertEquals(name, Some("Alice"))
        assertEquals(email, Some("alice@example.com"))
        assertEquals(newScore, 150L)
        assertEquals(hasPhone, false)
        assertEquals(allFields.size, 4)
        assertEquals(fieldCount, 4L)
      }
    }
  }
}
