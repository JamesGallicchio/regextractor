import sbt._
import Keys._

trait BuildSettings {
  private val org = "com.github.maqicode"
  private val ver = "0.2"
  private val sca = "2.11.7"

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := org,
    version      := ver,
    scalaVersion := sca,
    scalacOptions ++= Seq("-deprecation", "-feature", "-Xlint"),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "junit" % "junit" % "4.12" % "test"
    )
  )
}

object RegextractorBuild extends Build with BuildSettings {
  lazy val rootSettings = buildSettings
  // lazy val support = RootProject(uri("git://github.com/som-snytt/test-support.git"))
  lazy val root = Project("regextractor-root", file("."), settings = rootSettings) aggregate (core, util)
  lazy val core = Project("regextractor-core", file("core"), settings = buildSettings) dependsOn(util/*, support % "compile->test"*/)
  lazy val util = Project("regextractor-util", file("util"), settings = buildSettings) // dependsOn(support)
}
