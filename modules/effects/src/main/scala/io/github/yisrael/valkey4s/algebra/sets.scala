

package io.github.yisrael.valkey4s.algebra

trait SetCommands[F[_], K, V] extends SetGetter[F, K, V] with SetSetter[F, K, V] with SetDeletion[F, K, V] {
  def sIsMember(key: K, value: V): F[Boolean]
  def sMisMember(key: K, values: V*): F[List[Boolean]]
}

trait SetGetter[F[_], K, V] {
  def sCard(key: K): F[Long]
  def sDiff(keys: K*): F[Set[V]]
  def sInter(keys: K*): F[Set[V]]
  def sMembers(key: K): F[Set[V]]
  def sRandMember(key: K): F[Option[V]]
  def sRandMember(key: K, count: Long): F[List[V]]
  def sUnion(keys: K*): F[Set[V]]
  def sUnionStore(destination: K, keys: K*): F[Unit]
}

trait SetSetter[F[_], K, V] {
  def sAdd(key: K, values: V*): F[Long]
  def sDiffStore(destination: K, keys: K*): F[Long]
  def sInterStore(destination: K, keys: K*): F[Long]
  def sMove(source: K, destination: K, value: V): F[Boolean]
}

trait SetDeletion[F[_], K, V] {
  def sPop(key: K): F[Option[V]]
  def sPop(key: K, count: Long): F[Set[V]]
  def sRem(key: K, values: V*): F[Long]
}
