

package io.github.yisrael.valkey4s.extensions

import cats.effect.kernel.{Resource, Sync}
import cats.{ApplicativeThrow, Functor}
import io.github.yisrael.valkey4s.effects.ScriptOutputType

object luaScripting {

  final case class LuaScript(contents: String, sha: String)

  object LuaScript {

    def make[F[_]: Functor](redis: Scripting[F, _, _])(contents: String): F[LuaScript] =
      redis.digest(contents).map(sha => LuaScript(contents, sha))

    /** Helper to load a lua script from resources/lua/{resourceName}. The path to the lua scripts can be configured.
      * @param redis
      *   redis commands
      * @param resourceName
      *   filename of the lua script
      * @param pathToScripts
      *   path to the lua scripts
      * @return
      *   [[LuaScript]]
      */
    def loadFromResources[F[_]: Sync](redis: Scripting[F, _, _])(
        resourceName: String,
        pathToScripts: String = "lua"
    ): F[LuaScript] =
      Resource
        .fromAutoCloseable(
          Sync[F].blocking(
            scala.io.Source.fromResource(resource = s"$pathToScripts/$resourceName")
          )
        )
        .evalMap(fileSrc => Sync[F].blocking(fileSrc.mkString))
        .use(make(redis))

  }

  implicit class LuaScriptingExtensions[F[_]: ApplicativeThrow, K, V](redis: Scripting[F, K, V]) {

    /** Evaluate the cached lua script via it's sha. If the script is not cached, fallback to evaluating the script
      * directly.
      * @param luaScript
      *   the lua script with its content and sha
      * @param output
      *   output of script
      * @param keys
      *   keys to script
      * @param values
      *   values to script
      * @return
      *   ScriptOutputType
      */
    def evalLua(
        luaScript: LuaScript,
        output: ScriptOutputType[V],
        keys: List[K],
        values: List[V]
    ): F[output.R] =
      redis
        .evalSha(
          luaScript.sha,
          output,
          keys,
          values
        )
        .recoverWith { case _: RedisNoScriptException =>
          redis.eval(luaScript.contents, output, keys, values)
        }

  }
}
