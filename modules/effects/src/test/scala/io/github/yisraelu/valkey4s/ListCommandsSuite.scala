package dev.profunktor.valkey4cats

class ListCommandsSuite extends ValkeyTestSuite {

  test("LPUSH should add elements to the head of list") {
    valkeyClient.use { valkey =>
      for {
        length <- valkey.lpush("list1", "third", "second", "first")
        result <- valkey.lrange("list1", 0, -1)
        _ <- valkey.del("list1")
      } yield {
        assertEquals(length, 3L)
        assertEquals(result, List("first", "second", "third"))
      }
    }
  }

  test("RPUSH should add elements to the tail of list") {
    valkeyClient.use { valkey =>
      for {
        length <- valkey.rpush("list2", "first", "second", "third")
        result <- valkey.lrange("list2", 0, -1)
        _ <- valkey.del("list2")
      } yield {
        assertEquals(length, 3L)
        assertEquals(result, List("first", "second", "third"))
      }
    }
  }

  test("LPOP should remove and return first element") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list3", "first", "second", "third")
        popped <- valkey.lpop("list3")
        remaining <- valkey.lrange("list3", 0, -1)
        _ <- valkey.del("list3")
      } yield {
        assertEquals(popped, Some("first"))
        assertEquals(remaining, List("second", "third"))
      }
    }
  }

  test("LPOP should return None for non-existent list") {
    valkeyClient.use { valkey =>
      for {
        popped <- valkey.lpop("non-existent")
      } yield assertEquals(popped, None)
    }
  }

  test("RPOP should remove and return last element") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list4", "first", "second", "third")
        popped <- valkey.rpop("list4")
        remaining <- valkey.lrange("list4", 0, -1)
        _ <- valkey.del("list4")
      } yield {
        assertEquals(popped, Some("third"))
        assertEquals(remaining, List("first", "second"))
      }
    }
  }

  test("LPOPCOUNT should remove and return multiple elements from head") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list5", "a", "b", "c", "d", "e")
        popped <- valkey.lpopCount("list5", 3)
        remaining <- valkey.lrange("list5", 0, -1)
        _ <- valkey.del("list5")
      } yield {
        assertEquals(popped, List("a", "b", "c"))
        assertEquals(remaining, List("d", "e"))
      }
    }
  }

  test("RPOPCOUNT should remove and return multiple elements from tail") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list6", "a", "b", "c", "d", "e")
        popped <- valkey.rpopCount("list6", 3)
        remaining <- valkey.lrange("list6", 0, -1)
        _ <- valkey.del("list6")
      } yield {
        assertEquals(popped, List("e", "d", "c"))
        assertEquals(remaining, List("a", "b"))
      }
    }
  }

  test("LRANGE should return all elements with 0 -1") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list7", "a", "b", "c", "d", "e")
        result <- valkey.lrange("list7", 0, -1)
        _ <- valkey.del("list7")
      } yield {
        assertEquals(result, List("a", "b", "c", "d", "e"))
      }
    }
  }

  test("LRANGE should return specific range") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list8", "a", "b", "c", "d", "e")
        result <- valkey.lrange("list8", 1, 3)
        _ <- valkey.del("list8")
      } yield {
        assertEquals(result, List("b", "c", "d"))
      }
    }
  }

  test("LRANGE should return empty list for non-existent key") {
    valkeyClient.use { valkey =>
      for {
        result <- valkey.lrange("non-existent", 0, -1)
      } yield assertEquals(result, List.empty[String])
    }
  }

  test("LINDEX should return element at index") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list9", "a", "b", "c", "d", "e")
        elem0 <- valkey.lindex("list9", 0)
        elem2 <- valkey.lindex("list9", 2)
        elemLast <- valkey.lindex("list9", -1)
        _ <- valkey.del("list9")
      } yield {
        assertEquals(elem0, Some("a"))
        assertEquals(elem2, Some("c"))
        assertEquals(elemLast, Some("e"))
      }
    }
  }

  test("LINDEX should return None for out of range index") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list10", "a", "b", "c")
        result <- valkey.lindex("list10", 100)
        _ <- valkey.del("list10")
      } yield assertEquals(result, None)
    }
  }

  test("LLEN should return length of list") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list11", "a", "b", "c", "d", "e")
        length <- valkey.llen("list11")
        _ <- valkey.del("list11")
      } yield assertEquals(length, 5L)
    }
  }

  test("LLEN should return 0 for non-existent list") {
    valkeyClient.use { valkey =>
      for {
        length <- valkey.llen("non-existent")
      } yield assertEquals(length, 0L)
    }
  }

  test("LTRIM should trim list to specified range") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list12", "a", "b", "c", "d", "e")
        _ <- valkey.ltrim("list12", 1, 3)
        result <- valkey.lrange("list12", 0, -1)
        _ <- valkey.del("list12")
      } yield {
        assertEquals(result, List("b", "c", "d"))
      }
    }
  }

  test("LSET should set element at index") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list13", "a", "b", "c", "d", "e")
        _ <- valkey.lset("list13", 2, "CHANGED")
        result <- valkey.lrange("list13", 0, -1)
        _ <- valkey.del("list13")
      } yield {
        assertEquals(result, List("a", "b", "CHANGED", "d", "e"))
      }
    }
  }

  test("LREM should remove occurrences of element") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list14", "a", "b", "a", "c", "a", "d")
        count <- valkey.lrem("list14", 2, "a") // Remove first 2 occurrences
        result <- valkey.lrange("list14", 0, -1)
        _ <- valkey.del("list14")
      } yield {
        assertEquals(count, 2L)
        assertEquals(result, List("b", "c", "a", "d"))
      }
    }
  }

  test("LREM with count=0 should remove all occurrences") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list15", "a", "b", "a", "c", "a", "d")
        count <- valkey.lrem("list15", 0, "a")
        result <- valkey.lrange("list15", 0, -1)
        _ <- valkey.del("list15")
      } yield {
        assertEquals(count, 3L)
        assertEquals(result, List("b", "c", "d"))
      }
    }
  }

  test("LINSERT should insert element before pivot") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list16", "a", "b", "d", "e")
        length <- valkey.linsert("list16", before = true, "d", "c")
        result <- valkey.lrange("list16", 0, -1)
        _ <- valkey.del("list16")
      } yield {
        assertEquals(length, 5L)
        assertEquals(result, List("a", "b", "c", "d", "e"))
      }
    }
  }

  test("LINSERT should insert element after pivot") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list17", "a", "b", "c", "e")
        length <- valkey.linsert("list17", before = false, "c", "d")
        result <- valkey.lrange("list17", 0, -1)
        _ <- valkey.del("list17")
      } yield {
        assertEquals(length, 5L)
        assertEquals(result, List("a", "b", "c", "d", "e"))
      }
    }
  }

  test("LINSERT should return -1 when pivot not found") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list18", "a", "b", "c")
        length <- valkey.linsert("list18", before = true, "x", "y")
        _ <- valkey.del("list18")
      } yield assertEquals(length, -1L)
    }
  }

  test("LPOS should return index of first occurrence") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list19", "a", "b", "c", "b", "d")
        index <- valkey.lpos("list19", "b")
        _ <- valkey.del("list19")
      } yield assertEquals(index, Some(1L))
    }
  }

  test("LPOS should return None when element not found") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.rpush("list20", "a", "b", "c")
        index <- valkey.lpos("list20", "x")
        _ <- valkey.del("list20")
      } yield assertEquals(index, None)
    }
  }

  test("complex workflow: queue operations") {
    valkeyClient.use { valkey =>
      for {
        // Create a queue
        _ <- valkey.rpush("queue", "task1", "task2", "task3")

        // Check queue length
        length1 <- valkey.llen("queue")

        // Process first task (LPOP)
        task1 <- valkey.lpop("queue")

        // Add new tasks
        _ <- valkey.rpush("queue", "task4", "task5")

        // Check current queue
        currentQueue <- valkey.lrange("queue", 0, -1)

        // Process multiple tasks
        tasks <- valkey.lpopCount("queue", 2)

        // Final length
        finalLength <- valkey.llen("queue")

        // Cleanup
        _ <- valkey.del("queue")
      } yield {
        assertEquals(length1, 3L)
        assertEquals(task1, Some("task1"))
        assertEquals(currentQueue, List("task2", "task3", "task4", "task5"))
        assertEquals(tasks, List("task2", "task3"))
        assertEquals(finalLength, 2L)
      }
    }
  }

  test("complex workflow: stack operations (LIFO)") {
    valkeyClient.use { valkey =>
      for {
        // Push items (LPUSH acts as stack push)
        _ <- valkey.lpush("stack", "item1")
        _ <- valkey.lpush("stack", "item2")
        _ <- valkey.lpush("stack", "item3")

        // Pop items (LPOP acts as stack pop - LIFO order)
        item1 <- valkey.lpop("stack")
        item2 <- valkey.lpop("stack")
        item3 <- valkey.lpop("stack")

        // Cleanup
        _ <- valkey.del("stack")
      } yield {
        assertEquals(item1, Some("item3")) // Last in
        assertEquals(item2, Some("item2"))
        assertEquals(item3, Some("item1")) // First in
      }
    }
  }
}
