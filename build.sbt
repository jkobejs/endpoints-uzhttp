import EndpointsUzhttpSettings._

inThisBuild(
  List(
    organization := "io.github.jkobejs",
    homepage := Some(url("https://github.com/jkobejs/endpoints-uzhttp")),
    organizationName := "Josip Grgurica",
    startYear := Some(2020),
    licenses := List("MIT License" -> url("http://opensource.org/licenses/mit-license.php")),
    developers := List(
      Developer(
        "jkobejs",
        "Josip Grgurica",
        "josip.grgurica@gmail.com",
        url("https://github.com/jkobejs")
      )
    )
  )
)

lazy val `uzhttp-server` =
  project
    .in(file("server"))
    .settings(
      `scala 2.12 to latest`,
      name := "endpoints-uzhttp-server",
      libraryDependencies ++= Seq(
        "org.endpoints4s"   %% "algebra"     % endpointsVersion,
        "org.endpoints4s"   %% "openapi"     % endpointsVersion,
        "org.polynote"      %% "uzhttp"      % uzhttpVersion,
        "org.scalatest"     %% "scalatest"   % scalaTestVersion % Test,
        "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion  % Test,
        "com.typesafe.akka" %% "akka-actor"  % akkaActorVersion % Test,
        "com.typesafe.akka" %% "akka-stream" % akkaActorVersion % Test,
        "com.lihaoyi"       %% "ujson"       % ujsonVersion     % Test
      )
    )

lazy val documentation = project.in(file("documentation")).settings(noPublishSettings)

Global / onChangedBuildSource := ReloadOnSourceChanges

noPublishSettings

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
