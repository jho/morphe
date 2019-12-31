import Dependencies._

ThisBuild / scalaVersion     := "2.12.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "org.jho"
ThisBuild / organizationName := "morphe"

lazy val root = (project in file("."))
  .aggregate(core, circe)

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
  .settings(commonSettings)

lazy val circe = (project in file("circe"))
  .settings(
    name := "morphe-circe",
    libraryDependencies := Seq(
      circeCore,
      scalaTest % Test
    )
  )
  .settings(commonSettings)
  .dependsOn(core)

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
  .settings(publishSettings)*/

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/jho/morphe")),
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  bintrayVcsUrl := Some("git@github.com:jho/morphe.git")
)

lazy val compilerOptions = Seq(
  //"-Xfatal-warnings",
  "-Ywarn-unused-import",
  "-Yrangepos", // required by SemanticDB compiler plugin
  //"-deprecation", circe is throwing a bunch of these!!!
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ypartial-unification",
  "-Ywarn-dead-code",
  "-Ywarn-value-discard",
  "-Ypatmat-exhaust-depth",
  "off",
  "-Xfuture"
)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions,
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9"),
  //addCompilerPlugin(scalafixSemanticdb),
  //dependencyOverrides ++= Seq(),
  parallelExecution in Test := false,
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    Resolver.jcenterRepo
  )
) ++ publishSettings

//this has to be at the global level
//scalafixDependencies in ThisBuild += "com.nequissimus" %% "sort-imports" % "0.3.1"