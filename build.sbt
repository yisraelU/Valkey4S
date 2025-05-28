
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
    libraryDependencies ++= Seq(
      "io.valkey" % "valkey-glide" % "2.0.0-rc3" pomOnly(),
        "org.typelevel" %% "cats-core" % "2.13.0",
        "org.typelevel" %% "cats-effect" % "3.6.1",
  )
  )

lazy val effects = (projectMatrix in file("modules/effects"))
  .jvmPlatform(latest2ScalaVersions)
  .settings(
    name := "valkey4s-effects",
    libraryDependencies ++= Seq(
      "io.valkey" % "valkey-glide" % "2.0.0-rc3" pomOnly(),
      "org.typelevel" %% "cats-effect" % "3.6.1"
    )
    )
