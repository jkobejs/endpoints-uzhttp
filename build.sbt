import EndpointsToddlersSettings._
import sbt.{ uri, Project, ProjectRef }

val examples =
  project.in(file("examples")).settings(noPublishSettings)

lazy val `uzhttp-server` =
  project
    .in(file("server"))
    .settings(
      `scala 2.12 to latest`,
      name := "endpoints-uzhttp-server",
      libraryDependencies ++= Seq(
        "org.julienrf"      %% "endpoints-algebra" % endpointsVersion,
        "org.polynote"      %% "uzhttp"            % uzhttpVersion,
        "org.julienrf"      %% "endpoints-openapi" % endpointsVersion,
        "org.scalatest"     %% "scalatest"         % scalaTestVersion % Test,
        "com.typesafe.akka" %% "akka-http"         % akkaHttpVersion % Test,
        "com.typesafe.akka" %% "akka-actor"        % akkaActorVersion % Test,
        "com.typesafe.akka" %% "akka-stream"       % akkaActorVersion % Test,
        "com.lihaoyi"       %% "ujson"             % ujsonVersion % Test
      )
    )

Global / onChangedBuildSource := ReloadOnSourceChanges

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt it:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck it:scalafmtCheck")
