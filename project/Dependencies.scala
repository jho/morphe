import sbt._

object Dependencies {
  object Versions {
    val MagnoliaVersion = "0.12.+"
    val DrosteVersion = "0.8.+"
  }
  import Versions._

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"
  lazy val magnolia = "com.propensive" %% "magnolia" % MagnoliaVersion
  lazy val droste = "io.higherkindness" %% "droste-core" % DrosteVersion
  lazy val drosteMacros = "io.higherkindness" %% "droste-macros" % DrosteVersion

  def paradiseDep(scalaVersion: String): Seq[ModuleID] =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, minor)) if minor < 13 =>
        Seq(compilerPlugin("org.scalamacros" %% "paradise" % "2.1.1" cross CrossVersion.patch))
      case _ => Nil
    }
}
