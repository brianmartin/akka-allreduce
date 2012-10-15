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
      scalaVersion := "2.10.0-M7",
      resolvers += "Akka Respository" at "http://repo.akka.io/snapshots/",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" % "akka-actor" % "2.1-SNAPSHOT" cross CrossVersion.full,
        "com.typesafe.akka" %% "akka-remote" % "2.1-SNAPSHOT" cross CrossVersion.full
      ),
      publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
    )
  )
}
