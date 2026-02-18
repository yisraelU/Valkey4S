package dev.profunktor.valkey4cats.algebra

import dev.profunktor.valkey4cats.arguments.{FlushMode, InfoSection}

/** Server management command algebra */
trait ServerCommands[F[_], K, V] {

  /** Get information and statistics about the server
    *
    * @return Server information as a string
    */
  def info: F[String]

  /** Get specific sections of server information
    *
    * @param sections Set of information sections to retrieve
    * @return Server information for requested sections
    */
  def info(sections: Set[InfoSection]): F[String]

  /** Rewrite the configuration file with the in-memory configuration
    *
    * @return "OK" if successful
    */
  def configRewrite: F[String]

  /** Reset server statistics
    *
    * @return "OK" if successful
    */
  def configResetStat: F[String]

  /** Get configuration parameters
    *
    * @param parameters Configuration parameter names to retrieve
    * @return Map of parameter names to values
    */
  def configGet(parameters: Set[String]): F[Map[String, String]]

  /** Set configuration parameters
    *
    * @param parameters Map of parameter names to values to set
    * @return "OK" if successful
    */
  def configSet(parameters: Map[String, String]): F[String]

  /** Get current server time
    *
    * @return Tuple of (unix timestamp in seconds, microseconds)
    */
  def time: F[(Long, Long)]

  /** Get timestamp of the last successful save to disk
    *
    * @return Unix timestamp in seconds
    */
  def lastSave: F[Long]

  /** Remove all keys from all databases
    *
    * @return "OK" if successful
    */
  def flushAll: F[String]

  /** Remove all keys from all databases with specified mode
    *
    * @param mode Flush mode (sync or async)
    * @return "OK" if successful
    */
  def flushAll(mode: FlushMode): F[String]

  /** Remove all keys from the current database
    *
    * @return "OK" if successful
    */
  def flushDB: F[String]

  /** Remove all keys from the current database with specified mode
    *
    * @param mode Flush mode (sync or async)
    * @return "OK" if successful
    */
  def flushDB(mode: FlushMode): F[String]

  /** Display server version art (Easter egg command)
    *
    * @return ASCII art version display
    */
  def lolwut: F[String]

  /** Display server version art with specific version
    *
    * @param version Version number for the art
    * @return ASCII art version display
    */
  def lolwut(version: Int): F[String]

  /** Display server version art with version and optional parameters
    *
    * @param version Version number for the art
    * @param parameters Optional parameters for the art display
    * @return ASCII art version display
    */
  def lolwut(version: Int, parameters: List[Int]): F[String]

  /** Get the number of keys in the currently selected database
    *
    * @return Number of keys
    */
  def dbSize: F[Long]
}
