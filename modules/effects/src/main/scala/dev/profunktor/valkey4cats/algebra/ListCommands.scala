package dev.profunktor.valkey4cats.algebra

import dev.profunktor.valkey4cats.arguments.InsertPosition
import dev.profunktor.valkey4cats.model.ValkeyResponse
import dev.profunktor.valkey4cats.results.InsertResult

/** List commands for Valkey/Redis
  *
  * Lists are simple collections of string elements sorted by insertion order.
  */
trait ListCommands[F[_], K, V] {

  /** Insert all the specified values at the head of the list stored at key.
    * If key does not exist, it is created as empty list before performing the push operations.
    *
    * @param key The key of the list
    * @param elements Elements to push
    * @return The length of the list after the push operations
    */
  def lpush(key: K, elements: V*): F[ValkeyResponse[Long]]

  /** Insert all the specified values at the tail of the list stored at key.
    * If key does not exist, it is created as empty list before performing the push operations.
    *
    * @param key The key of the list
    * @param elements Elements to push
    * @return The length of the list after the push operations
    */
  def rpush(key: K, elements: V*): F[ValkeyResponse[Long]]

  /** Remove and return the first element of the list stored at key
    *
    * @param key The key of the list
    * @return The value of the first element, or None when key does not exist
    */
  def lpop(key: K): F[ValkeyResponse[Option[V]]]

  /** Remove and return the last element of the list stored at key
    *
    * @param key The key of the list
    * @return The value of the last element, or None when key does not exist
    */
  def rpop(key: K): F[ValkeyResponse[Option[V]]]

  /** Remove and return up to count elements from the head of the list
    *
    * @param key The key of the list
    * @param count The number of elements to pop
    * @return List of popped elements
    */
  def lpopCount(key: K, count: Long): F[ValkeyResponse[List[V]]]

  /** Remove and return up to count elements from the tail of the list
    *
    * @param key The key of the list
    * @param count The number of elements to pop
    * @return List of popped elements
    */
  def rpopCount(key: K, count: Long): F[ValkeyResponse[List[V]]]

  /** Get the specified range of elements from the list stored at key.
    * The offsets start and stop are zero-based indexes.
    *
    * @param key The key of the list
    * @param start Start index (inclusive)
    * @param stop Stop index (inclusive)
    * @return List of elements in the specified range
    */
  def lrange(key: K, start: Long, stop: Long): F[ValkeyResponse[List[V]]]

  /** Get the element at index in the list stored at key.
    * The index is zero-based.
    *
    * @param key The key of the list
    * @param index The index
    * @return The requested element, or None when index is out of range
    */
  def lindex(key: K, index: Long): F[ValkeyResponse[Option[V]]]

  /** Get the length of the list stored at key
    *
    * @param key The key of the list
    * @return The length of the list, or 0 when key does not exist
    */
  def llen(key: K): F[ValkeyResponse[Long]]

  /** Trim an existing list so that it will contain only the specified range of elements.
    * Both start and stop are zero-based indexes.
    *
    * @param key The key of the list
    * @param start Start index (inclusive)
    * @param stop Stop index (inclusive)
    */
  def ltrim(key: K, start: Long, stop: Long): F[ValkeyResponse[Unit]]

  /** Set the list element at index to element.
    *
    * @param key The key of the list
    * @param index The index
    * @param element The element to set
    */
  def lset(key: K, index: Long, element: V): F[ValkeyResponse[Unit]]

  /** Remove the first count occurrences of elements equal to element from the list.
    * The count argument influences the operation:
    * - count > 0: Remove elements equal to element moving from head to tail
    * - count < 0: Remove elements equal to element moving from tail to head
    * - count = 0: Remove all elements equal to element
    *
    * @param key The key of the list
    * @param count The number of occurrences to remove
    * @param element The element to remove
    * @return The number of removed elements
    */
  def lrem(key: K, count: Long, element: V): F[ValkeyResponse[Long]]

  /** Insert element in the list stored at key either before or after the reference value pivot.
    *
    * @param key The key of the list
    * @param position Insert position (Before or After)
    * @param pivot The reference value
    * @param element The element to insert
    * @return InsertResult indicating outcome
    */
  def linsert(
      key: K,
      position: InsertPosition,
      pivot: V,
      element: V
  ): F[ValkeyResponse[InsertResult]]

  /** Return the index of the first occurrence of element in the list.
    *
    * @param key The key of the list
    * @param element The element to search for
    * @return The index of the element, or None if not found
    */
  def lpos(key: K, element: V): F[ValkeyResponse[Option[Long]]]
}
