import sbt.ScriptedPlugin
import sbt.ScriptedPlugin._
import scoverage.ScoverageSbtPlugin.ScoverageKeys._

lazy val buildSettings = Seq(
  organization := "ch.epfl.scala",
  assemblyJarName in assembly := "scalafix.jar",
  // See core/src/main/scala/ch/epfl/scala/Versions.scala
  version :=  scalafix.Versions.nightly,
  scalaVersion :=  scalafix.Versions.scala,
  updateOptions := updateOptions.value.withCachedResolution(true)
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Xlint"
)

lazy val commonSettings = Seq(
  ScoverageSbtPlugin.ScoverageKeys.coverageExcludedPackages :=
      ".*Versions;scalafix\\.(sbt|util)",
  triggeredMessage in ThisBuild := Watched.clearWhenTriggered,
  scalacOptions in (Compile, console) := compilerOptions :+ "-Yrepl-class-based",
  testOptions in Test += Tests.Argument("-oD")
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishMavenStyle := true,
  publishArtifact := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/scalacenter/scalafix")),
  autoAPIMappings := true,
  apiURL := Some(url("https://scalacenter.github.io/scalafix/docs/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/scalacenter/scalafix"),
      "scm:git:git@github.com:scalacenter/scalafix.git"
    )
  ),
  pomExtra :=
    <developers>
      <developer>
        <id>olafurpg</id>
        <name>Ólafur Páll Geirsson</name>
        <url>https://geirsson.com</url>
      </developer>
    </developers>
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {}
)

lazy val allSettings = commonSettings ++ buildSettings ++ publishSettings


lazy val root = project.in(file("."))
  .settings(moduleName := "scalafix")
  .settings(allSettings)
  .settings(noPublish)
  .settings(
    initialCommands in console :=
      """
        |import scala.meta._
        |import scalafix._
      """.stripMargin
  )
  .aggregate(core, cli, sbtScalafix)
  .dependsOn(core)

lazy val core = project
  .settings(allSettings)
  .settings(
    moduleName := "scalafix-core",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "sourcecode" % "0.1.2",
      "org.scalameta" %% "scalameta" % "1.0.0",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,

      // Test dependencies
      "org.scalatest" %% "scalatest" % "3.0.0" % "test",
      "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0" % "test"
    )
  )

lazy val cli = project
    .settings(allSettings)
    .settings(
      moduleName := "scalafix-cli",
      mainClass in assembly := Some("scalafix.cli.Cli"),
      libraryDependencies ++= Seq(
        "com.github.scopt" %% "scopt" % "3.5.0"
      )
    )
    .dependsOn(core % "compile->compile;test->test")

lazy val sbtScalafix = project
    .settings(allSettings)
    .settings(ScriptedPlugin.scriptedSettings)
    .settings(
      sbtPlugin := true,
      coverageHighlighting := false,
      scalaVersion := "2.10.5",
      moduleName := "sbt-scalafix",
      sources in Compile +=
          baseDirectory.value / "../core/src/main/scala/scalafix/Versions.scala",
      scriptedLaunchOpts := Seq(
        "-Dplugin.version=" + version.value,
        // .jvmopts is ignored, simulate here
        "-XX:MaxPermSize=256m", "-Xmx2g", "-Xss2m"
      ),
      scriptedBufferLog := false
    )
