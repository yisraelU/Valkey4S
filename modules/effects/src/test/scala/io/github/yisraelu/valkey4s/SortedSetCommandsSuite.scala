package dev.profunktor.valkey4cats

class SortedSetCommandsSuite extends ValkeyTestSuite {

  test("ZADD should add members with scores") {
    valkeyClient.use { valkey =>
      for {
        count <- valkey.zadd("zset1", Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0))
        members <- valkey.zrange("zset1", 0, -1)
        _ <- valkey.del("zset1")
      } yield {
        assertEquals(count, 3L)
        assertEquals(members, List("a", "b", "c"))
      }
    }
  }

  test("ZADD should update scores for existing members") {
    valkeyClient.use { valkey =>
      for {
        count1 <- valkey.zadd("zset2", Map("a" -> 1.0, "b" -> 2.0))
        count2 <- valkey.zadd("zset2", Map("b" -> 5.0)) // Update b's score
        members <- valkey.zrange("zset2", 0, -1)
        _ <- valkey.del("zset2")
      } yield {
        assertEquals(count1, 2L)
        assertEquals(count2, 0L) // 0 because b already existed
        assertEquals(members, List("a", "b")) // b now has higher score
      }
    }
  }

  test("ZREM should remove members from sorted set") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd(
          "zset3",
          Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0, "d" -> 4.0)
        )
        count <- valkey.zrem("zset3", "b", "d")
        members <- valkey.zrange("zset3", 0, -1)
        _ <- valkey.del("zset3")
      } yield {
        assertEquals(count, 2L)
        assertEquals(members, List("a", "c"))
      }
    }
  }

  test("ZRANGE should return members in score order") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd(
          "zset4",
          Map("alice" -> 100.0, "bob" -> 50.0, "charlie" -> 150.0)
        )
        members <- valkey.zrange("zset4", 0, -1)
        _ <- valkey.del("zset4")
      } yield {
        assertEquals(members, List("bob", "alice", "charlie"))
      }
    }
  }

  test("ZRANGE should return subset by index") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd(
          "zset5",
          Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0, "d" -> 4.0, "e" -> 5.0)
        )
        members <- valkey.zrange("zset5", 1, 3)
        _ <- valkey.del("zset5")
      } yield {
        assertEquals(members, List("b", "c", "d"))
      }
    }
  }

  test("ZRANGEWITHSCORES should return members with scores") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd("zset6", Map("a" -> 1.5, "b" -> 2.5, "c" -> 3.5))
        membersWithScores <- valkey.zrangeWithScores("zset6", 0, -1)
        _ <- valkey.del("zset6")
      } yield {
        assertEquals(
          membersWithScores,
          List(("a", 1.5), ("b", 2.5), ("c", 3.5))
        )
      }
    }
  }

  test("ZSCORE should return score of member") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd("zset7", Map("alice" -> 95.5, "bob" -> 87.3))
        score <- valkey.zscore("zset7", "alice")
        _ <- valkey.del("zset7")
      } yield {
        assertEquals(score, Some(95.5))
      }
    }
  }

  test("ZSCORE should return None for non-existent member") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd("zset8", Map("alice" -> 95.5))
        score <- valkey.zscore("zset8", "bob")
        _ <- valkey.del("zset8")
      } yield assertEquals(score, None)
    }
  }

  test("ZMSCORE should return scores for multiple members") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd("zset9", Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0))
        scores <- valkey.zmscore("zset9", "a", "x", "c")
        _ <- valkey.del("zset9")
      } yield {
        assertEquals(scores, List(Some(1.0), None, Some(3.0)))
      }
    }
  }

  test("ZCARD should return number of members") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd(
          "zset10",
          Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0, "d" -> 4.0)
        )
        cardinality <- valkey.zcard("zset10")
        _ <- valkey.del("zset10")
      } yield assertEquals(cardinality, 4L)
    }
  }

  test("ZCARD should return 0 for non-existent sorted set") {
    valkeyClient.use { valkey =>
      for {
        cardinality <- valkey.zcard("non-existent")
      } yield assertEquals(cardinality, 0L)
    }
  }

  test("ZRANK should return rank of member (0-based, ascending)") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd(
          "zset11",
          Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0, "d" -> 4.0)
        )
        rank <- valkey.zrank("zset11", "c")
        _ <- valkey.del("zset11")
      } yield assertEquals(rank, Some(2L)) // 0: a, 1: b, 2: c
    }
  }

  test("ZRANK should return None for non-existent member") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd("zset12", Map("a" -> 1.0, "b" -> 2.0))
        rank <- valkey.zrank("zset12", "x")
        _ <- valkey.del("zset12")
      } yield assertEquals(rank, None)
    }
  }

  test("ZREVRANK should return rank in descending order") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd(
          "zset13",
          Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0, "d" -> 4.0)
        )
        rank <- valkey.zrevrank("zset13", "c")
        _ <- valkey.del("zset13")
      } yield assertEquals(rank, Some(1L)) // Reversed: 0: d, 1: c, 2: b, 3: a
    }
  }

  test("ZINCRBY should increment member score") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd("zset14", Map("player1" -> 100.0))
        newScore <- valkey.zincrby("zset14", 50.0, "player1")
        score <- valkey.zscore("zset14", "player1")
        _ <- valkey.del("zset14")
      } yield {
        assertEquals(newScore, 150.0)
        assertEquals(score, Some(150.0))
      }
    }
  }

  test("ZINCRBY should initialize member if doesn't exist") {
    valkeyClient.use { valkey =>
      for {
        newScore <- valkey.zincrby("zset15", 42.5, "new-member")
        _ <- valkey.del("zset15")
      } yield assertEquals(newScore, 42.5)
    }
  }

  test("ZCOUNT should count members in score range") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd(
          "zset16",
          Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0, "d" -> 4.0, "e" -> 5.0)
        )
        count <- valkey.zcount("zset16", 2.0, 4.0)
        _ <- valkey.del("zset16")
      } yield assertEquals(count, 3L) // b, c, d
    }
  }

  test("ZPOPMIN should remove and return member with lowest score") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd("zset17", Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0))
        popped <- valkey.zpopmin("zset17")
        remaining <- valkey.zrange("zset17", 0, -1)
        _ <- valkey.del("zset17")
      } yield {
        assertEquals(popped, Some(("a", 1.0)))
        assertEquals(remaining, List("b", "c"))
      }
    }
  }

  test("ZPOPMIN should return None for non-existent sorted set") {
    valkeyClient.use { valkey =>
      for {
        popped <- valkey.zpopmin("non-existent")
      } yield assertEquals(popped, None)
    }
  }

  test(
    "ZPOPMINCOUNT should remove and return multiple members with lowest scores"
  ) {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd(
          "zset18",
          Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0, "d" -> 4.0)
        )
        popped <- valkey.zpopminCount("zset18", 2)
        remaining <- valkey.zrange("zset18", 0, -1)
        _ <- valkey.del("zset18")
      } yield {
        assertEquals(popped, List(("a", 1.0), ("b", 2.0)))
        assertEquals(remaining, List("c", "d"))
      }
    }
  }

  test("ZPOPMAX should remove and return member with highest score") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd("zset19", Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0))
        popped <- valkey.zpopmax("zset19")
        remaining <- valkey.zrange("zset19", 0, -1)
        _ <- valkey.del("zset19")
      } yield {
        assertEquals(popped, Some(("c", 3.0)))
        assertEquals(remaining, List("a", "b"))
      }
    }
  }

  test(
    "ZPOPMAXCOUNT should remove and return multiple members with highest scores"
  ) {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd(
          "zset20",
          Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0, "d" -> 4.0)
        )
        popped <- valkey.zpopmaxCount("zset20", 2)
        remaining <- valkey.zrange("zset20", 0, -1)
        _ <- valkey.del("zset20")
      } yield {
        assertEquals(popped, List(("d", 4.0), ("c", 3.0)))
        assertEquals(remaining, List("a", "b"))
      }
    }
  }

  test("ZRANDMEMBER should return random member without removing") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd("zset21", Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0))
        member <- valkey.zrandmember("zset21")
        card <- valkey.zcard("zset21")
        _ <- valkey.del("zset21")
      } yield {
        assert(member.isDefined)
        assert(Set("a", "b", "c").contains(member.get))
        assertEquals(card, 3L) // Size unchanged
      }
    }
  }

  test("ZRANDMEMBER should return None for non-existent sorted set") {
    valkeyClient.use { valkey =>
      for {
        member <- valkey.zrandmember("non-existent")
      } yield assertEquals(member, None)
    }
  }

  test("ZRANDMEMBERCOUNT should return multiple random members") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd(
          "zset22",
          Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0, "d" -> 4.0)
        )
        members <- valkey.zrandmemberCount("zset22", 2)
        card <- valkey.zcard("zset22")
        _ <- valkey.del("zset22")
      } yield {
        assertEquals(members.length, 2)
        assertEquals(card, 4L) // Size unchanged
      }
    }
  }

  test("ZRANDMEMBERWITHSCORES should return random members with scores") {
    valkeyClient.use { valkey =>
      for {
        _ <- valkey.zadd("zset23", Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0))
        membersWithScores <- valkey.zrandmemberWithScores("zset23", 2)
        _ <- valkey.del("zset23")
      } yield {
        assertEquals(membersWithScores.length, 2)
        membersWithScores.foreach { case (member, score) =>
          assert(Set("a", "b", "c").contains(member))
          assert(score >= 1.0 && score <= 3.0)
        }
      }
    }
  }

  test("complex workflow: leaderboard system") {
    valkeyClient.use { valkey =>
      for {
        // Initialize player scores
        _ <- valkey.zadd(
          "leaderboard",
          Map(
            "Alice" -> 1000.0,
            "Bob" -> 1500.0,
            "Charlie" -> 800.0,
            "Diana" -> 1200.0,
            "Eve" -> 900.0
          )
        )

        // Get top 3 players
        top3 <- valkey.zrangeWithScores(
          "leaderboard",
          -3,
          -1
        ) // Last 3 (highest scores)

        // Get Alice's rank (0-based from lowest, so we use zrevrank for highest-first)
        aliceRevRank <- valkey.zrevrank("leaderboard", "Alice")

        // Alice wins a game and gains 300 points
        aliceNewScore <- valkey.zincrby("leaderboard", 300.0, "Alice")

        // Get updated top 3
        updatedTop3 <- valkey.zrangeWithScores("leaderboard", -3, -1)

        // Count players with score >= 1000
        highScorers <- valkey.zcount("leaderboard", 1000.0, Double.MaxValue)

        // Get total player count
        totalPlayers <- valkey.zcard("leaderboard")

        // Cleanup
        _ <- valkey.del("leaderboard")
      } yield {
        // Original top 3: Bob (1500), Diana (1200), Alice (1000)
        assertEquals(top3.map(_._1), List("Alice", "Diana", "Bob"))

        // Alice was originally 3rd from top (0: Bob, 1: Diana, 2: Alice)
        assertEquals(aliceRevRank, Some(2L))

        // Alice's new score
        assertEquals(aliceNewScore, 1300.0)

        // Updated top 3: Bob (1500), Alice (1300), Diana (1200)
        assertEquals(updatedTop3.map(_._1), List("Diana", "Alice", "Bob"))

        // High scorers: Bob, Alice, Diana (now 3 players >= 1000)
        assertEquals(highScorers, 3L)

        // Total players
        assertEquals(totalPlayers, 5L)
      }
    }
  }

  test("complex workflow: priority queue") {
    valkeyClient.use { valkey =>
      for {
        // Add tasks with priorities (lower score = higher priority)
        _ <- valkey.zadd(
          "tasks",
          Map(
            "critical-bug" -> 1.0,
            "feature-request" -> 5.0,
            "documentation" -> 10.0,
            "security-fix" -> 2.0
          )
        )

        // Get highest priority task
        topTask <- valkey.zpopmin("tasks")

        // Add urgent task
        _ <- valkey.zadd("tasks", Map("urgent-patch" -> 1.5))

        // Process next 2 tasks
        nextTasks <- valkey.zpopminCount("tasks", 2)

        // Check remaining tasks
        remaining <- valkey.zrangeWithScores("tasks", 0, -1)

        // Cleanup
        _ <- valkey.del("tasks")
      } yield {
        assertEquals(topTask, Some(("critical-bug", 1.0)))
        assertEquals(nextTasks.map(_._1), List("urgent-patch", "security-fix"))
        assertEquals(
          remaining.map(_._1),
          List("feature-request", "documentation")
        )
      }
    }
  }
}
