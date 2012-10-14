import sbt._
import sbt.Keys._

object AkkaAllreduceBuild extends Build {

  lazy val akkaAllreduce = Project(
    id = "akka-allreduce",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "Akka Allreduce",
      organization := "com.github.brianmartin",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.9.2",
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0.1"
    )
  )
}
