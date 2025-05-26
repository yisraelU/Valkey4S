
Global / onChangedBuildSource := ReloadOnSourceChanges
val Scala213 = "2.13.16"
val Scala3 = "3.5.2"
val latest2ScalaVersions = List(Scala213, Scala3)
ThisBuild / scalaVersion := Scala213 // the default S

ThisBuild / tlBaseVersion := "0.0" // your current series x.y
lazy val root = project
  .in(file("."))
