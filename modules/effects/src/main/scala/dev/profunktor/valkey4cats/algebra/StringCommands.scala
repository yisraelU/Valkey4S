package dev.profunktor.valkey4cats.algebra

import dev.profunktor.valkey4cats.arguments.SetOptions
import dev.profunktor.valkey4cats.model.ValkeyResponse
import dev.profunktor.valkey4cats.results.SetResult

/** String/Key-Value command algebra */
trait StringCommands[F[_], K, V] {

  /** Get the value of a key
    *
    * @param key The key to get
    * @return Some(value) if key exists, None otherwise
    */
  def get(key: K): F[ValkeyResponse[Option[V]]]

  /** Set the value of a key
    *
    * @param key The key to set
    * @param value The value to set
    */
  def set(key: K, value: V): F[ValkeyResponse[Unit]]

  /** Set the value of a key with options
    *
    * @param key The key to set
    * @param value The value to set
    * @param options Set options (expiry, conditional set, etc.)
    * @return SetResult indicating outcome
    */
  def set(
      key: K,
      value: V,
      options: SetOptions
  ): F[ValkeyResponse[SetResult[V]]]

  /** Get multiple values by keys
    *
    * @param keys Set of keys to get
    * @return Map of key-value pairs for keys that exist
    */
  def mGet(keys: Set[K]): F[ValkeyResponse[Map[K, V]]]

  /** Set multiple key-value pairs
    *
    * @param keyValues Map of key-value pairs to set
    */
  def mSet(keyValues: Map[K, V]): F[ValkeyResponse[Unit]]

  /** Increment a key's integer value by 1
    *
    * @param key The key to increment
    * @return The new value after increment
    */
  def incr(key: K): F[ValkeyResponse[Long]]

  /** Increment a key's integer value by a specific amount
    *
    * @param key The key to increment
    * @param amount The amount to increment by
    * @return The new value after increment
    */
  def incrBy(key: K, amount: Long): F[ValkeyResponse[Long]]

  /** Decrement a key's integer value by 1
    *
    * @param key The key to decrement
    * @return The new value after decrement
    */
  def decr(key: K): F[ValkeyResponse[Long]]

  /** Decrement a key's integer value by a specific amount
    *
    * @param key The key to decrement
    * @param amount The amount to decrement by
    * @return The new value after decrement
    */
  def decrBy(key: K, amount: Long): F[ValkeyResponse[Long]]

  /** Append a value to a key
    *
    * @param key The key to append to
    * @param value The value to append
    * @return The length of the string after the append
    */
  def append(key: K, value: V): F[ValkeyResponse[Long]]

  /** Get the length of a string value
    *
    * @param key The key to get length for
    * @return The length of the string, or 0 if key doesn't exist
    */
  def strlen(key: K): F[ValkeyResponse[Long]]
}
