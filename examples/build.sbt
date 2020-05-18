import EndpointsToddlersSettings._

val `uzhttp-server` = LocalProject("uzhttp-server")

val `example-uzhttp-server` =
  project
    .in(file("uzhttp-server"))
    .settings(
      noPublishSettings,
      `scala 2.12 to latest`,
      publishArtifact := false,
      libraryDependencies ++= Seq(
        "org.julienrf" %% "endpoints-algebra"             % endpointsVersion,
        "org.julienrf" %% "endpoints-json-schema-generic" % endpointsVersion
      )
    )
    .dependsOn(`uzhttp-server`)
