import scala.sys.process._

enablePlugins(TutPlugin)

lazy val `presentation` = project.in(file("."))
  .settings(projectLayout ++ compilerOptions ++ compilerPlugins)
  .settings(

    tutSourceDirectory := baseDirectory.value / "src",
    tutTargetDirectory := baseDirectory.value / "target",

    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core"   % "1.1.0",
      "org.typelevel" %% "cats-effect" % "0.10.1",
      // Test dependencies:
      "org.scalatest" %% "scalatest" % "3.0.4"  % "test",
    ),

    tut := {
      val log = streams.value.log
      val tutRes = tut.value
      log.info("Running pandoc...")
      Seq(
        "pandoc",
        //"--filter pandoc-include-code",
        "-t beamer",
        "--pdf-engine=xelatex",
        "-H customizations.tex",
        "-o talk.pdf target/talk.md",
      ).mkString(" ") !! log
      tutRes
    }
  )

lazy val projectLayout = Seq(
  scalaSource in Compile       := baseDirectory.value / "src" / "main",
  scalaSource in Test          := baseDirectory.value / "test" / "main",
)

lazy val compilerOptions = Seq(
  scalacOptions ++= Seq(
    "-Ypartial-unification",
    "-Xfatal-warnings",
    "-feature",
    "-deprecation",
    "-language:higherKinds",
    "-language:implicitConversions",
  ),
  scalacOptions in (Test, compile) ~= (_ filterNot (_ == "-Ywarn-unused"))
)

lazy val compilerPlugins = Seq(
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"),
  addCompilerPlugin(
    "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
  ),
)
