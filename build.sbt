import Dependencies._

ThisBuild / scalaVersion     := "2.12.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "org.jho"
ThisBuild / organizationName := "morphe"

lazy val root = (project in file("."))
  .aggregate(core)

lazy val core = project
  .settings(
    name := "morphe-core",
    libraryDependencies := Seq(
      magnolia,
      droste,
      drosteMacros,
      scalaTest % Test,
      compilerPlugin(
        "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch)
    )
  )
  .settings(publishSettings)

/*
lazy val avro = (project in file("avro"))
  .settings(
    name := "morphe-avro"
  )
  .dependesOn(core)
  .settings(testDependencies)
  .settings(publishSettings)

lazy val spark = (project in file("."))
  .settings(
    name := "morphe-spark"
  )
  .settings(testDependencies)
  .settings(publishSettings)

lazy val avro = (project in file("."))
  .settings(
    name := "morphe-spark"
  )
  .settings(testDependencies)
  .settings(publishSettings)*/

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/jho/morphe")),
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  bintrayVcsUrl := Some("git@github.com:jho/morphe.git")
)



