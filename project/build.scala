
import sbt._
import Keys._

trait BuildSettings {
  private val org = "com.github.maqicode"
  private val ver = "0.1"
  //private val sca = "2.11.0-M8"
  private val sca = "2.11.0-SNAPSHOT"
  //private val sca = "2.11.0-20140115-161534-681308a3aa"
  private val hom = "/home/apm"


  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := org,
    version      := ver,
    scalaVersion := sca,
    scalaHome    := Some(file("/home/apm/clones/scala/build/pack")),
    resolvers += "Local Maven Repository" at s"file://$hom/.m2/repository",
    scalacOptions ++= Seq(/*"-Xdev",*/ /*"-Ymacro-debug-verbose",*/ /*"-Xprint:typer,uncurry,lambdalift",*/ "-deprecation", "-feature", "-Xlint", "-Xfatal-warnings"),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "com.novocode" % "junit-interface" % "0.10" % "test",
      "junit" % "junit" % "4.10" % "test"
    )
  )
}

object RegextractorBuild extends Build with BuildSettings {

  lazy val rootSettings = buildSettings

  //lazy val support = RootProject(file("../test-support"))
  lazy val support = RootProject(uri("git://github.com/som-snytt/test-support.git"))

  lazy val root = Project("root", file("."), settings = rootSettings) aggregate (core, util)

  lazy val core = Project("core", file("core"), settings = buildSettings) dependsOn(util, support % "compile->test")

  lazy val util = Project("util", file("util"), settings = buildSettings) dependsOn(support)
}
