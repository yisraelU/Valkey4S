package io.github.yisraelu.valkey4s.algebra

import io.github.yisraelu.valkey4s.arguments.ZAddOptions

/** Sorted Set commands for Valkey/Redis
  *
  * Sorted sets are collections of unique string elements where each element
  * has an associated score. Elements are ordered by score.
  */
trait SortedSetCommands[F[_], K, V] {

  /** Add one or more members to a sorted set, or update its score if it already exists
    *
    * @param key The key of the sorted set
    * @param membersScores Map of members to scores
    * @return The number of elements added to the sorted set (not including updates)
    */
  def zadd(key: K, membersScores: Map[V, Double]): F[Long]

  /** Add one or more members to a sorted set with options
    *
    * @param key The key of the sorted set
    * @param membersScores Map of members to scores
    * @param options ZADD options (NX, XX, GT, LT, CH)
    * @return The number of elements added/changed depending on options
    */
  def zadd(key: K, membersScores: Map[V, Double], options: ZAddOptions): F[Long]

  /** Remove one or more members from a sorted set
    *
    * @param key The key of the sorted set
    * @param members Members to remove
    * @return The number of members removed from the sorted set
    */
  def zrem(key: K, members: V*): F[Long]

  /** Get the specified range of elements in a sorted set by index.
    * Both start and stop are zero-based indexes.
    *
    * @param key The key of the sorted set
    * @param start Start index (inclusive)
    * @param stop Stop index (inclusive)
    * @return List of elements in the specified range
    */
  def zrange(key: K, start: Long, stop: Long): F[List[V]]

  /** Get the specified range of elements with scores in a sorted set by index.
    * Both start and stop are zero-based indexes.
    *
    * @param key The key of the sorted set
    * @param start Start index (inclusive)
    * @param stop Stop index (inclusive)
    * @return List of (element, score) pairs in the specified range
    */
  def zrangeWithScores(key: K, start: Long, stop: Long): F[List[(V, Double)]]

  /** Get the score associated with a member in a sorted set
    *
    * @param key The key of the sorted set
    * @param member The member
    * @return The score of the member, or None if member doesn't exist
    */
  def zscore(key: K, member: V): F[Option[Double]]

  /** Get the scores associated with multiple members in a sorted set
    *
    * @param key The key of the sorted set
    * @param members The members
    * @return List of scores (None for members that don't exist)
    */
  def zmscore(key: K, members: V*): F[List[Option[Double]]]

  /** Get the number of members in a sorted set
    *
    * @param key The key of the sorted set
    * @return The cardinality (number of elements) of the sorted set
    */
  def zcard(key: K): F[Long]

  /** Get the rank of member in the sorted set (ascending order, 0-based)
    *
    * @param key The key of the sorted set
    * @param member The member
    * @return The rank of the member, or None if member doesn't exist
    */
  def zrank(key: K, member: V): F[Option[Long]]

  /** Get the rank of member in the sorted set (descending order, 0-based)
    *
    * @param key The key of the sorted set
    * @param member The member
    * @return The rank of the member, or None if member doesn't exist
    */
  def zrevrank(key: K, member: V): F[Option[Long]]

  /** Increment the score of a member in a sorted set
    *
    * @param key The key of the sorted set
    * @param increment The amount to increment
    * @param member The member
    * @return The new score of the member
    */
  def zincrby(key: K, increment: Double, member: V): F[Double]

  /** Count the members in a sorted set with scores within the given range
    *
    * @param key The key of the sorted set
    * @param min Minimum score (inclusive)
    * @param max Maximum score (inclusive)
    * @return The number of elements in the specified score range
    */
  def zcount(key: K, min: Double, max: Double): F[Long]

  /** Remove and return the member with the lowest score from a sorted set
    *
    * @param key The key of the sorted set
    * @return The removed (member, score) pair, or None if the sorted set is empty
    */
  def zpopmin(key: K): F[Option[(V, Double)]]

  /** Remove and return up to count members with the lowest scores from a sorted set
    *
    * @param key The key of the sorted set
    * @param count The number of members to pop
    * @return List of removed (member, score) pairs
    */
  def zpopminCount(key: K, count: Long): F[List[(V, Double)]]

  /** Remove and return the member with the highest score from a sorted set
    *
    * @param key The key of the sorted set
    * @return The removed (member, score) pair, or None if the sorted set is empty
    */
  def zpopmax(key: K): F[Option[(V, Double)]]

  /** Remove and return up to count members with the highest scores from a sorted set
    *
    * @param key The key of the sorted set
    * @param count The number of members to pop
    * @return List of removed (member, score) pairs
    */
  def zpopmaxCount(key: K, count: Long): F[List[(V, Double)]]

  /** Get one random member from a sorted set
    *
    * @param key The key of the sorted set
    * @return A random member, or None if the sorted set is empty
    */
  def zrandmember(key: K): F[Option[V]]

  /** Get one or more random members from a sorted set
    *
    * @param key The key of the sorted set
    * @param count The number of members to return
    * @return List of random members
    */
  def zrandmemberCount(key: K, count: Long): F[List[V]]

  /** Get one or more random members with scores from a sorted set
    *
    * @param key The key of the sorted set
    * @param count The number of members to return
    * @return List of random (member, score) pairs
    */
  def zrandmemberWithScores(key: K, count: Long): F[List[(V, Double)]]
}
