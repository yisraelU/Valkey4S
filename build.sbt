
Global / onChangedBuildSource := ReloadOnSourceChanges

// Scala versions
val Scala213 = "2.13.18"
val Scala3 = "3.5.2"
val supportedScalaVersions = List(Scala213, Scala3)

// Build settings
ThisBuild / tlBaseVersion := "0.0"
ThisBuild / scalaVersion := Scala213

def pred[A](p: Boolean, t: => Seq[A], f: => Seq[A]): Seq[A] =
  if (p) t else f

def getVersion(strVersion: String): Option[(Long, Long)] = CrossVersion.partialVersion(strVersion)

// Common settings for all modules
lazy val commonSettings = Seq(
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) =>
        Seq(
          "-Xsource:3",
          "-Wunused:imports",
          "-Wunused:privates",
          "-Wunused:locals",
          "-Wvalue-discard",
        )
      case Some((3, _)) =>
        Seq(
          "-Xmax-inlines", "64",
          "-Wunused:imports",
        )
      case _ => Seq.empty
    }
  },
  Compile / unmanagedSourceDirectories ++= {
    getVersion(scalaVersion.value) match {
      case Some((2, 12)) => Seq("scala-2.12", "scala-2")
      case Some((2, 13)) => Seq("scala-2.13+", "scala-2")
      case _             => Seq("scala-2.13+", "scala-3")
    }
  }.map(baseDirectory.value / "src" / "main" / _),
)
lazy val root = project
  .in(file("."))
  .settings(
    name := "Valkey4S",
    organization := "io.github.yisraelu",
    publish / skip := true,
  )
  .aggregate(core.projectRefs ++ effects.projectRefs ++ examples.projectRefs : _*)



// Helper to determine the OS classifier for Glide JNI bindings
lazy val osClassifier = {
  val os = sys.props("os.name").toLowerCase match {
    case mac if mac.contains("mac") => "osx"
    case linux if linux.contains("linux") => "linux"
    case win if win.contains("win") => "windows"
    case other => throw new Exception(s"Unsupported OS: $other")
  }

  val arch = sys.props("os.arch").toLowerCase match {
    case "amd64" | "x86_64" => "x86_64"
    case "aarch64" | "arm64" => "aarch_64"
    case other => throw new Exception(s"Unsupported architecture: $other")
  }

  s"$os-$arch"
}


lazy val core = (projectMatrix in file("modules/glide-core"))
  .jvmPlatform(scalaVersions = supportedScalaVersions)
  .settings(commonSettings)
  .settings(
    name := "valkey4s-core",
    libraryDependencies ++= Seq(
      "io.valkey" % "valkey-glide" % "2.2.4" classifier osClassifier,
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.typelevel" %% "cats-effect" % "3.6.3",
      "org.typelevel" %% "literally" % "1.2.0",
// Test dependencies
      "org.scalameta" %% "munit" % "1.0.0" % Test,
      "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test,
      "org.testcontainers" % "testcontainers" % "1.19.3" % Test,
    ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) => Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value)
      case _ => Seq.empty
    })
  )

lazy val effects = (projectMatrix in file("modules/glide-effects"))
  .dependsOn(core % "compile->compile;test->test")
  .jvmPlatform(scalaVersions = supportedScalaVersions)
  .settings(commonSettings)
  .settings(
    name := "valkey4s-effects",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.6.3",
      // Test dependencies
      "org.scalameta" %% "munit" % "1.2.1" % Test,
      "org.typelevel" %% "munit-cats-effect" % "2.1.0" % Test,
      "org.testcontainers" % "testcontainers" % "2.0.3" % Test,
    )
  )

lazy val examples = (projectMatrix in file("examples"))
  .dependsOn(core, effects)
  .jvmPlatform(scalaVersions = supportedScalaVersions)
  .settings(commonSettings)
  .settings(
    name := "valkey4s-examples",
    publish / skip := true,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.6.3"
    )
  )

// Convenience commands for cross-compilation
addCommandAlias("compileAll", ";+core/compile ;+effects/compile ;+examples/compile")
addCommandAlias("testAll", ";+core/test ;+effects/test")
addCommandAlias("testAllQuick", ";+core/testQuick ;+effects/testQuick")
