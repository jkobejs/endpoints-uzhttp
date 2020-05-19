import EndpointsToddlersSettings._

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

lazy val documentation = project
  .in(file("documentation"))
  .settings(
    `scala 2.12 to latest`,
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(
      `uzhttp-server`
    ),
    siteSubdirName in ScalaUnidoc := "latest/api",
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc),
    git.remoteRepo := "git@github.com:jkobejs/endpoints-uzhttp.git"
  )
  .enablePlugins(ScalaUnidocPlugin)
  .enablePlugins(GhpagesPlugin)

Global / onChangedBuildSource := ReloadOnSourceChanges

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt it:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck it:scalafmtCheck")
