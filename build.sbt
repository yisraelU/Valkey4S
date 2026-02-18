import com.scalapenos.sbt.prompt.SbtPrompt.autoImport.*
import com.scalapenos.sbt.prompt.*

Global / onChangedBuildSource := ReloadOnSourceChanges

// Scala version
val Scala3 = "3.5.2"
ThisBuild / mimaBaseVersion := "1.8.0"
ThisBuild / scalaVersion := Scala3
Test / parallelExecution := false

promptTheme := PromptTheme(
  List(
    text("[sbt] ", fg(105)),
    text(_ => "valkey4cats", fg(15)).padRight(" λ ")
  )
)

// Common settings for all modules
val commonSettings = Seq(
  organizationName := "Valkey client for Cats Effect & Fs2",
  startYear := Some(2018),
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  headerLicense := Some(HeaderLicense.ALv2("2018-2025", "ProfunKtor")),
  testFrameworks += new TestFramework("munit.Framework"),
  resolvers += "Apache public" at "https://repository.apache.org/content/groups/public/",
  scalacOptions ++= Seq("-source:3.0-migration", "-Xmax-inlines", "64"),
  Compile / doc / sources := (Compile / doc / sources).value,
  Compile / doc / scalacOptions ++= Seq("-groups", "-implicits"),
  autoAPIMappings := true,
  scalafmtOnCompile := true,
  scmInfo := Some(
    ScmInfo(url("https://github.com/profunktor/valkey4cats"), "scm:git:git@github.com:profunktor/valkey4cats.git")
  )
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "valkey4cats",
    organization := "dev.profunktor",
    publish / skip := true,
  )
  .aggregate(core, effects, examples)

lazy val core = project
  .in(file("modules/core"))
  .settings(commonSettings)
  .settings(
    name := "valkey4cats-core",
    libraryDependencies ++= Dependencies.Groups.core ++ Dependencies.Groups.test,
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

lazy val effects = project
  .in(file("modules/effects"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(
    name := "valkey4cats-effects",
    libraryDependencies ++= Dependencies.Groups.effects ++ Dependencies.Groups.test
  )

lazy val log4Cats = project
  .in(file("modules/log4cats"))
  .dependsOn(core)
  .settings(
    name := "valkey4cats-log4cats",
    libraryDependencies ++= Dependencies.Groups.log4cats
  )

lazy val examples = project
  .in(file("modules/examples"))
  .dependsOn(core, effects)
  .settings(commonSettings)
  .settings(
    name := "valkey4cats-examples",
    publish / skip := true,
    libraryDependencies ++= Dependencies.Groups.examples
  )

// Convenience commands
addCommandAlias("compileAll", ";core/compile ;effects/compile ;examples/compile")
addCommandAlias("testAll", ";core/test ;effects/test")
addCommandAlias("testAllQuick", ";core/testQuick ;effects/testQuick")
