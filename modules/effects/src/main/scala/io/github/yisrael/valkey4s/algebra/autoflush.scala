
package io.github.yisrael.valkey4s.algebra

trait PipelineCommands[F[_]] extends AutoFlush[F]

trait AutoFlush[F[_]] {
  def enableAutoFlush: F[Unit]
  def disableAutoFlush: F[Unit]
  def flushCommands: F[Unit]
}
