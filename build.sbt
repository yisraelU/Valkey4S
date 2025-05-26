
Global / onChangedBuildSource := ReloadOnSourceChanges
val Scala213 = List("2.13.16")
val Scala3 = List("3.5.2")
val latest2ScalaVersions = Scala213 ++ Scala3

ThisBuild / tlBaseVersion := "0.0" // your current series x.y
lazy val root = project
  .in(file("."))
  .settings(
    name := "Valkey4S",
    organization := "io.github.yisraelu",
  )
  .aggregate(core.projectRefs : _*)

lazy val core = (projectMatrix in file("modules/core"))
  .jvmPlatform(latest2ScalaVersions)
  .settings(
    name := "valkey4s-core",
    libraryDependencies += "io.valkey" % "valkey-glide" % "2.0.0-rc3" pomOnly()
  )
