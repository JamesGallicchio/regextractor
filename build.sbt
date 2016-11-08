import sbt._

lazy val commonSettings = Seq(
  organization         := "io.github.soc",
   homepage            := Some(url("https://github.com/soc/regextractor")),
  licenses             := Seq("Apache License 2.0" -> url("https://opensource.org/licenses/Apache-2.0")),
  version              := "0.2",
  scalaVersion         := "2.11.8",
  crossScalaVersions   := Seq("2.11.8", "2.12.0"),
  scalacOptions       ++= Seq("-deprecation", "-feature", "-Xlint"),
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect"   % scalaVersion.value,
    "com.novocode"   % "junit-interface" % "0.11"              % "test",
    "junit"          % "junit"           % "4.12"              % "test"
  ),
  publishArtifact in Test := false,
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
         Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
  pomExtra := pomData,
  pomIncludeRepository := { _ => false }
)

lazy val root   = Project("regextractor-root", file("."),       settings = commonSettings).aggregate(core, macros)
  .settings(
    // No, SBT, we don't want any artifacts for root.
    // No, not even an empty jar.
    // Invoking Cthulhu:
    packageBin in Global := file(""),
    packagedArtifacts    := Map(),
    publish              := {},
    publishLocal         := {},
    publishArtifact      := false,
    Keys.`package`       := file("")
  )
lazy val core   = Project("regextractor", file("core"),         settings = commonSettings).dependsOn(macros/*, support % "compile->test"*/)
lazy val macros = Project("regextractor-macro", file("macros"), settings = commonSettings)//.dependsOn(support)
//lazy val support = RootProject(uri("git://github.com/som-snytt/test-support.git"))

lazy val pomData =
  <scm>
    <url>git@github.com:soc/regextractor.git</url>
    <connection>scm:git:git@github.com:soc/regextractor.git</connection>
  </scm>
  <developers>
    <developer>
      <id>som-snytt</id>
      <name>A. P. Marki</name>
      <url>https://github.com/som-snytt</url>
      <roles>
        <role>Project Lead</role>
      </roles>
    </developer>
  </developers>
