package io.github.yisraelu.valkey4s.connection

sealed abstract case class ValkeyClient private(underlying: JRedisClient, uri: GlideUri)

