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
      scalaVersion := "2.9.1",
      resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" % "akka-actor" % "2.0.3",
        "com.typesafe.akka" % "akka-remote" % "2.0.3"
      ),
      publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
    )
  )
}
