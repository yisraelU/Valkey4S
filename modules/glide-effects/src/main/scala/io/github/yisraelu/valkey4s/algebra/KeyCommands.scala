package io.github.yisraelu.valkey4s.algebra

/** Key management command algebra */
trait KeyCommands[F[_], K, V] {

  /** Delete one or more keys
    *
    * @param keys Keys to delete
    * @return Number of keys deleted
    */
  def del(keys: K*): F[Long]

  /** Check if a key exists
    *
    * @param key The key to check
    * @return true if key exists, false otherwise
    */
  def exists(key: K): F[Boolean]

  /** Check if multiple keys exist
    *
    * @param keys Keys to check
    * @return Number of keys that exist
    */
  def existsMany(keys: K*): F[Long]
}
