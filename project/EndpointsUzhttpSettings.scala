import sbt._
import sbt.Keys._

object EndpointsUzhttpSettings {
  val commonSettings = Seq(
    organization := "io.github.jkobejs",
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-unchecked",
      "-language:implicitConversions",
      "-Xlint",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen"
    ) ++
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => Seq("-Xsource:2.14")
        case _ =>
          Seq(
            "-Yno-adapted-args",
            "-Ywarn-unused-import",
            "-Ywarn-value-discard",
            "-Xexperimental",
            "-Xfuture",
            "-language:higherKinds"
          )
      })
  )

  val `scala 2.12 to latest` = Seq(
    scalaVersion := "2.13.1",
    crossScalaVersions := Seq("2.13.1", "2.12.10")
  )

  val noPublishSettings = commonSettings ++ Seq(
    publishArtifact := false,
    publish := { () },
    publishLocal := { () }
  )

  // --- Common dependencies
  val circeVersion     = "0.13.0"
  val endpointsVersion = "0.15.0+46-37dde73f"
  val akkaHttpVersion  = "10.1.12"
  val akkaActorVersion = "2.6.5"
  val scalaTestVersion = "3.1.2"
  val uzhttpVersion    = "0.2.3"
  val ujsonVersion     = "1.1.0"

  val macroParadiseDependency = Seq(
    scalacOptions in Compile ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => "-Ymacro-annotations" :: Nil
        case _                       => Nil
      }
    },
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => Nil
        case _ =>
          compilerPlugin(
            "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
          ) :: Nil
      }
    }
  )
}
