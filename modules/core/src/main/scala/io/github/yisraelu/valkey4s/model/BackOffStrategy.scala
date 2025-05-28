package io.github.yisraelu.valkey4s.model


case class BackOffStrategy (numRetries:Int, factor: Int, exponentBase: Int,jitterPercent:Int)