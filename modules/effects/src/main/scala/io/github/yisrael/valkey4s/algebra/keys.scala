

package io.github.yisrael.valkey4s.algebra

import java.time.Instant
import dev.profunktor.redis4cats.data.KeyScanCursor
import io.github.yisrael.valkey4s.effects.{ CopyArgs, ExpireExistenceArg, KeyScanArgs, RedisType, RestoreArgs, ScanArgs }

import scala.concurrent.duration.FiniteDuration

trait KeyCommands[F[_], K] {
  def copy(source: K, destination: K): F[Boolean]
  def copy(source: K, destination: K, copyArgs: CopyArgs): F[Boolean]
  def del(k: K, keys: K*): F[Long]
  def dump(key: K): F[Option[Array[Byte]]]
  def exists(key: K, keys: K*): F[Boolean]
  def expire(key: K, expiresIn: FiniteDuration): F[Boolean]
  def expire(key: K, expiresIn: FiniteDuration, expireExistenceArg: ExpireExistenceArg): F[Boolean]
  def expireAt(key: K, at: Instant): F[Boolean]
  def expireAt(key: K, at: Instant, expireExistenceArg: ExpireExistenceArg): F[Boolean]
  def objectIdletime(key: K): F[Option[FiniteDuration]]
  def persist(key: K): F[Boolean]
  def pttl(key: K): F[Option[FiniteDuration]]
  def randomKey: F[Option[K]]
  // restores a key with the given serialized value, previously obtained using DUMP without a ttl
  def restore(key: K, value: Array[Byte]): F[Unit]
  def restore(key: K, value: Array[Byte], restoreArgs: RestoreArgs): F[Unit]
  def scan: F[KeyScanCursor[K]]
  @deprecated("In favor of scan(cursor: KeyScanCursor[K])", since = "0.10.4")
  def scan(cursor: Long): F[KeyScanCursor[K]]
  def scan(previous: KeyScanCursor[K]): F[KeyScanCursor[K]]
  @deprecated("In favor of scan(keyScanArgs: KeyScanArgs)", since = "1.7.2")
  def scan(scanArgs: ScanArgs): F[KeyScanCursor[K]]
  def scan(keyScanArgs: KeyScanArgs): F[KeyScanCursor[K]]
  @deprecated("In favor of scan(cursor: KeyScanCursor[K], scanArgs: ScanArgs)", since = "0.10.4")
  def scan(cursor: Long, scanArgs: ScanArgs): F[KeyScanCursor[K]]
  @deprecated("In favor of scan(previous: KeyScanCursor[K], keyScanArgs: KeyScanArgs)", since = "1.7.2")
  def scan(previous: KeyScanCursor[K], scanArgs: ScanArgs): F[KeyScanCursor[K]]
  def scan(cursor: KeyScanCursor[K], keyScanArgs: KeyScanArgs): F[KeyScanCursor[K]]
  def typeOf(key: K): F[Option[RedisType]]
  def ttl(key: K): F[Option[FiniteDuration]]
  // This command is very similar to DEL: it removes the specified keys. Just like DEL a key is ignored if it does not exist. However the command performs the actual memory reclaiming in a different thread, so it is not blocking, while DEL is. This is where the command name comes from: the command just unlinks the keys from the keyspace. The actual removal will happen later asynchronously.
  def unlink(key: K*): F[Long]

}
