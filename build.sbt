import com.scalapenos.sbt.prompt.SbtPrompt.autoImport._
import com.scalapenos.sbt.prompt._

Global / onChangedBuildSource := ReloadOnSourceChanges

// Scala versions
val Scala213 = "2.13.18"
val Scala3 = "3.5.2"
val supportedScalaVersions = List(Scala213, Scala3)
ThisBuild / mimaBaseVersion := "1.8.0"
// Build settings
ThisBuild / scalaVersion := Scala213
Test / parallelExecution := false

promptTheme := PromptTheme(
  List(
    text("[sbt] ", fg(105)),
    text(_ => "valkey4cats", fg(15)).padRight(" λ ")
  )
)

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
    name := "valkey4cats",
    organization := "dev.profunktor",
    publish / skip := true,
  )
  .aggregate(core.projectRefs ++ effects.projectRefs ++ examples.projectRefs : _*)



lazy val core = (projectMatrix in file("modules/core"))
  .jvmPlatform(scalaVersions = supportedScalaVersions)
  .settings(commonSettings)
  .settings(
    name := "valkey4cats-core",
    libraryDependencies ++= {
      // Detect current platform for development and testing
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
      val classifier = s"$os-$arch"

      Seq(
        // Glide with current platform's classifier for development
        // This will be marked as optional in the published POM
        ("io.valkey" % "valkey-glide" % "2.2.4" classifier classifier).withConfigurations(Some("compile->default")),
        "org.typelevel" %% "cats-core" % "2.13.0",
        "org.typelevel" %% "cats-effect" % "3.6.3",
        "org.typelevel" %% "literally" % "1.2.0",
        // Test dependencies
        "org.scalameta" %% "munit" % "1.2.1" % Test,
        "org.typelevel" %% "munit-cats-effect" % "2.1.0" % Test,
        "org.testcontainers" % "testcontainers" % "2.0.3" % Test,
      ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 13)) => Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value)
        case _ => Seq.empty
      })
    },
    // Modify POM to mark Glide as optional and remove platform-specific classifier
    pomPostProcess := { node =>
      import scala.xml._
      import scala.xml.transform._
      new RuleTransformer(new RewriteRule {
        override def transform(n: Node): Seq[Node] = n match {
          case elem: Elem if elem.label == "dependency" =>
            val artifactId = (elem \ "artifactId").text
            if (artifactId == "valkey-glide") {
              // Mark Glide as optional and remove classifier
              val filteredChildren = elem.child.filterNot(_.label == "classifier")
              val newChildren = filteredChildren ++ <optional>true</optional>
              elem.copy(child = newChildren)
            } else {
              elem
            }
          case other => other
        }
      }).transform(node).head
    }
  )

lazy val effects = (projectMatrix in file("modules/effects"))
  .dependsOn(core % "compile->compile;test->test")
  .jvmPlatform(scalaVersions = supportedScalaVersions)
  .settings(commonSettings)
  .settings(
    name := "valkey4cats-effects",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.6.3",
      // Test dependencies
      "org.scalameta" %% "munit" % "1.2.1" % Test,
      "org.typelevel" %% "munit-cats-effect" % "2.1.0" % Test,
      "org.testcontainers" % "testcontainers" % "2.0.3" % Test,
    )
  )

lazy val log4Cats = (projectMatrix in file("modules/log4cats"))
  .jvmPlatform(supportedScalaVersions)
  .dependsOn(core)
  .settings(
    name := "valkey4cats-log4cats",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "log4cats-core" % "2.7.1"
    )
  )
lazy val examples = (projectMatrix in file("modules/examples"))
  .dependsOn(core, effects)
  .jvmPlatform(scalaVersions = supportedScalaVersions)
  .settings(commonSettings)
  .settings(
    name := "valkey4cats-examples",
    publish / skip := true,
    libraryDependencies ++= {
      // For examples only, we need the actual Glide binary for the current platform
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
      val classifier = s"$os-$arch"

      Seq(
        "io.valkey" % "valkey-glide" % "2.2.4" classifier classifier,
        "org.typelevel" %% "cats-effect" % "3.6.3"
      )
    }
  )

// Convenience commands for cross-compilation
addCommandAlias("compileAll", ";+core/compile ;+effects/compile ;+examples/compile")
addCommandAlias("testAll", ";+core/test ;+effects/test")
addCommandAlias("testAllQuick", ";+core/testQuick ;+effects/testQuick")
