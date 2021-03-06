name := """play-getting-started"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "org.scalaz" %% "scalaz-core" % "7.2.2",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % "test",
  ws
)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _ )
