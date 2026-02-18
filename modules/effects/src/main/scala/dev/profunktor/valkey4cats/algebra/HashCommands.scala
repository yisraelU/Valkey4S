package dev.profunktor.valkey4cats.algebra

import dev.profunktor.valkey4cats.model.ValkeyResponse

/** Hash commands for Valkey/Redis
  *
  * Hashes are maps between string fields and string values,
  * making them perfect for representing objects.
  */
trait HashCommands[F[_], K, V] {

  /** Set field in the hash stored at key to value.
    * If key does not exist, a new key holding a hash is created.
    * If field already exists in the hash, it is overwritten.
    *
    * @param key The key of the hash
    * @param fieldValues Map of field-value pairs to set
    * @return The number of fields that were added
    */
  def hset(key: K, fieldValues: Map[K, V]): F[ValkeyResponse[Long]]

  /** Get the value of a hash field
    *
    * @param key The key of the hash
    * @param field The field in the hash
    * @return The value associated with field, or None when field is not present
    */
  def hget(key: K, field: K): F[ValkeyResponse[Option[V]]]

  /** Get all the fields and values in a hash
    *
    * @param key The key of the hash
    * @return Map of fields and their values stored in the hash
    */
  def hgetall(key: K): F[ValkeyResponse[Map[K, V]]]

  /** Get the values of all the given hash fields
    *
    * @param key The key of the hash
    * @param fields The fields in the hash
    * @return List of values associated with the given fields, in the same order
    */
  def hmget(key: K, fields: K*): F[ValkeyResponse[List[Option[V]]]]

  /** Delete one or more hash fields
    *
    * @param key The key of the hash
    * @param fields The fields to delete
    * @return The number of fields that were removed from the hash
    */
  def hdel(key: K, fields: K*): F[ValkeyResponse[Long]]

  /** Determine if a hash field exists
    *
    * @param key The key of the hash
    * @param field The field in the hash
    * @return true if the hash contains field, false otherwise
    */
  def hexists(key: K, field: K): F[ValkeyResponse[Boolean]]

  /** Get all the fields in a hash
    *
    * @param key The key of the hash
    * @return List of fields in the hash, or an empty list when key does not exist
    */
  def hkeys(key: K): F[ValkeyResponse[List[K]]]

  /** Get all the values in a hash
    *
    * @param key The key of the hash
    * @return List of values in the hash, or an empty list when key does not exist
    */
  def hvals(key: K): F[ValkeyResponse[List[V]]]

  /** Get the number of fields in a hash
    *
    * @param key The key of the hash
    * @return Number of fields in the hash, or 0 when key does not exist
    */
  def hlen(key: K): F[ValkeyResponse[Long]]

  /** Increment the integer value of a hash field by the given number
    *
    * @param key The key of the hash
    * @param field The field in the hash
    * @param increment The increment
    * @return The value at field after the increment
    */
  def hincrBy(key: K, field: K, increment: Long): F[ValkeyResponse[Long]]

  /** Increment the float value of a hash field by the given amount
    *
    * @param key The key of the hash
    * @param field The field in the hash
    * @param increment The increment
    * @return The value at field after the increment
    */
  def hincrByFloat(
      key: K,
      field: K,
      increment: Double
  ): F[ValkeyResponse[Double]]

  /** Set the value of a hash field, only if the field does not exist
    *
    * @param key The key of the hash
    * @param field The field in the hash
    * @param value The value to set
    * @return true if field is a new field in the hash and value was set,
    *         false if field already exists and the value was not set
    */
  def hsetnx(key: K, field: K, value: V): F[ValkeyResponse[Boolean]]

  /** Get the string length of the field value in the hash
    *
    * @param key The key of the hash
    * @param field The field in the hash
    * @return The string length of the value, or 0 when field is not present
    */
  def hstrlen(key: K, field: K): F[ValkeyResponse[Long]]

  /** Get one random field from a hash
    *
    * @param key The key of the hash
    * @return A random field from the hash, or None when key does not exist
    */
  def hrandfield(key: K): F[ValkeyResponse[Option[K]]]

  /** Get multiple random fields from a hash
    *
    * @param key The key of the hash
    * @param count The number of fields to return
    * @return List of random fields from the hash
    */
  def hrandfieldWithCount(key: K, count: Long): F[ValkeyResponse[List[K]]]

  /** Get multiple random fields with their values from a hash
    *
    * @param key The key of the hash
    * @param count The number of fields to return
    * @return List of (field, value) pairs
    */
  def hrandfieldWithCountWithValues(
      key: K,
      count: Long
  ): F[ValkeyResponse[List[(K, V)]]]
}
